package amarr.indexer

import amarr.indexer.search.SearchQuery
import amarr.indexer.caps.Caps
import amarr.indexer.torznab.TorznabFeed

interface Indexer {

    /**
     * Given a paginated query, returns a [TorznabFeed] with the results.
     */
    suspend fun search(query: SearchQuery, offset: Int, limit: Int, cat: List<Int>): TorznabFeed

    /**
     * Returns the capabilities of this indexer.
     */
    suspend fun capabilities(): Caps

}