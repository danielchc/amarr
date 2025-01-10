package amarr.indexer.implementations.amule

import amarr.MagnetLink
import amarr.indexer.search.SearchFormat
import amarr.indexer.search.SearchQuery
import amarr.indexer.search.SearchType
import amarr.indexer.Indexer
import amarr.indexer.caps.Caps
import amarr.indexer.filters.MediaFilter
import amarr.indexer.torznab.TorznabFeed
import amarr.indexer.torznab.TorznabFeed.Channel.Item
import io.ktor.util.logging.*
import jamule.AmuleClient
import jamule.response.SearchResultsResponse.SearchFile

class AmuleIndexer(private val amuleClient: AmuleClient, private val log: Logger) : Indexer {

    override suspend fun search(query: SearchQuery, offset: Int, limit: Int, cat: List<Int>): TorznabFeed {
        // https://wiki.amule.org/wiki/Search_regexp

        if (query.q.isBlank()) {
            log.debug("Empty query, returning empty response")
            return EMPTY_QUERY_RESPONSE
        }

        val regexpQuery: String = when (query.searchType) {
            SearchType.TV -> "%s AND (%s)".format(query.getCleanedQuery(),
                (SearchFormat.epSearchFormat.map { k -> k.format(query.season, query.episode) }).joinToString( " OR " ))
            else -> query.getCleanedQuery()
        }

        return buildFeed(amuleClient.searchSync(regexpQuery).getOrThrow().files, offset, limit)
    }

    private fun buildFeed(items: List<SearchFile>, offset: Int, limit: Int) = TorznabFeed(
        channel = TorznabFeed.Channel(
            response = TorznabFeed.Channel.Response(
                offset = offset,
                total = items.size
            ),
            item = items
                .drop(offset)
                .take(limit)
                .filter { result -> MediaFilter.mediaFilter(result.fileName) }
                .map { result ->
                    Item(
                        title = result.fileName,
                        enclosure = Item.Enclosure(
                            url = MagnetLink.forAmarr(result.hash, result.fileName, result.sizeFull).toString(),
                            length = result.sizeFull
                        ),
                        attributes = listOf(
                            Item.TorznabAttribute("category", "1"),
                            Item.TorznabAttribute("seeders", result.completeSourceCount.toString()),
                            Item.TorznabAttribute("peers", result.sourceCount.toString()),
                            Item.TorznabAttribute("size", result.sizeFull.toString())
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
                    Item(
                        title = "No query provided",
                        enclosure = Item.Enclosure("http://mock.url", 0),
                        attributes = listOf(
                            Item.TorznabAttribute("category", "1"),
                            Item.TorznabAttribute("size", "0")
                        )
                    )
                )
            )
        )
    }

    override suspend fun capabilities(): Caps = Caps()
}
