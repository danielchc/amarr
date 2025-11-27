package amarr.indexer.implementations.amule

import amarr.MagnetLink
import amarr.indexer.Indexer
import amarr.indexer.cache.CacheStore
import amarr.indexer.caps.Caps
import amarr.indexer.filters.MediaFilter
import amarr.indexer.search.SearchFormat
import amarr.indexer.search.SearchQuery
import amarr.indexer.search.SearchType
import amarr.indexer.torznab.TorznabFeed
import io.ktor.util.logging.Logger
import jamule.AmuleClient
import jamule.response.SearchResultsResponse
import kotlin.system.measureTimeMillis

class AmuleIndexer(private val amuleClient: AmuleClient, private val log: Logger, private val cacheStore: CacheStore) :
    Indexer {

    override suspend fun search(
        query: SearchQuery,
        mediaFilter: MediaFilter,
        offset: Int,
        limit: Int,
        cat: List<Int>
    ): TorznabFeed {
        // https://wiki.amule.org/wiki/Search_regexp

        if (query.q.isBlank()) {
            log.debug("Empty query, returning empty response")
            return EMPTY_QUERY_RESPONSE
        }

        val regexpQuery: String = when (query.searchType) {
            SearchType.TV -> "%s AND (%s)".format(
                query.getCleanedQuery(),
                (SearchFormat.Companion.epSearchFormat.map { k -> k.format(query.season, query.episode) }).joinToString(
                    " OR "
                )
            )

            else -> query.getCleanedQuery()
        }

        log.info("Starting search for query: '{}', offset: {}, limit: {}", regexpQuery, offset, limit)

        val (items, duration) = measureTimedValue {
            cacheStore.get<List<SearchResultsResponse.SearchFile>>(regexpQuery)?.also {
                log.info("Cache hit for query: $regexpQuery")
            } ?: run {
                log.info("Cache miss for query: $regexpQuery — fetching")
                val fetched = amuleClient.searchSync(regexpQuery).getOrThrow().files.filter { result ->
                    mediaFilter.filter(result.fileName)
                }
                cacheStore.put(regexpQuery, fetched)
                fetched
            }
        }


        log.info("End search for query: '{}': {} results in {} ms", regexpQuery, items.size, duration)


        return buildFeed(items, offset, limit)
    }

    private fun buildFeed(items: List<SearchResultsResponse.SearchFile>, offset: Int, limit: Int) = TorznabFeed(
        channel = TorznabFeed.Channel(
            response = TorznabFeed.Channel.Response(
                offset = offset,
                total = items.size
            ),
            item = items
                .drop(offset)
                .take(limit)
                .map { result ->
                    TorznabFeed.Channel.Item(
                        title = result.fileName,
                        enclosure = TorznabFeed.Channel.Item.Enclosure(
                            url = MagnetLink.Companion.forAmarr(result.hash, result.fileName, result.sizeFull)
                                .toString(),
                            length = result.sizeFull
                        ),
                        attributes = listOf(
                            TorznabFeed.Channel.Item.TorznabAttribute("category", "1"),
                            TorznabFeed.Channel.Item.TorznabAttribute("seeders", result.completeSourceCount.toString()),
                            TorznabFeed.Channel.Item.TorznabAttribute("peers", result.sourceCount.toString()),
                            TorznabFeed.Channel.Item.TorznabAttribute("size", result.sizeFull.toString())
                        )
                    )
                }
        )
    )

    companion object {
        private val EMPTY_QUERY_RESPONSE = TorznabFeed(
            channel = TorznabFeed.Channel(
                response = TorznabFeed.Channel.Response(offset = 0, total = 1),
                item = listOf(
                    TorznabFeed.Channel.Item(
                        title = "No query provided",
                        enclosure = TorznabFeed.Channel.Item.Enclosure("http://mock.url", 0),
                        attributes = listOf(
                            TorznabFeed.Channel.Item.TorznabAttribute("category", "1"),
                            TorznabFeed.Channel.Item.TorznabAttribute("size", "0")
                        )
                    )
                )
            )
        )
    }

    override suspend fun capabilities(): Caps = Caps()

    inline fun <T> measureTimedValue(block: () -> T): Pair<T, Long> {
        val start = System.currentTimeMillis()
        val result = block()
        val duration = System.currentTimeMillis() - start
        return result to duration
    }
}