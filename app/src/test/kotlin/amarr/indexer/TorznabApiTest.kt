package amarr.indexer

import amarr.indexer.implementations.amule.AmuleIndexer
import amarr.indexer.caps.Caps
import amarr.indexer.search.SearchQuery
import amarr.indexer.search.SearchType
import amarr.indexer.torznab.TorznabFeed
import amarr.indexer.torznab.torznabApi
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.ktor.client.request.*
import io.ktor.server.testing.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk

class TorznabApiTest : StringSpec({
    val amuleIndexer = mockk<AmuleIndexer>()
    val searchQuery = mockk<SearchQuery>()
    "should throw exception when missing action" {
        testApplication {
            application {
                torznabApi(amuleIndexer)
            }
            shouldThrow<IllegalArgumentException> { client.get("/api") }
        }
    }

    "should throw exception on unknown action" {
        testApplication {
            application {
                torznabApi(amuleIndexer)
            }
            shouldThrow<IllegalArgumentException> { client.get("/api?t=unknown") }
        }
    }

    "should get capabilities from amule indexer when called on /api" {
        testApplication {
            application {
                torznabApi(amuleIndexer)
            }
            coEvery { amuleIndexer.capabilities() } returns Caps()
            client.get("/api?t=caps")
            coVerify { amuleIndexer.capabilities() }
        }
    }

//    "should pass query, offset and limits to amule indexer when called on /api" {
//        testApplication {
//            application {
//                torznabApi(amuleIndexer)
//            }
//            coEvery {
//                amuleIndexer.search(
//                    searchQuery,
//                    0,
//                    100,
//                    listOf()
//                )
//            } returns TorznabFeed(
//                channel = TorznabFeed.Channel(
//                    response = TorznabFeed.Channel.Response(offset = 0, total = 0),
//                    item = emptyList()
//                )
//            )
//            client.get("/api?t=search&q=test&offset=0&limit=100")
//            coVerify { amuleIndexer.search(searchQuery, 0, 100, listOf()) }
//        }
//    }
})