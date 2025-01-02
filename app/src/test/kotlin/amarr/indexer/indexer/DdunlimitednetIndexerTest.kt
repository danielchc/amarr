package amarr.indexer.indexer

import amarr.indexer.indexer.ddunlimitednet.DdunlimitednetClient
import amarr.indexer.indexer.ddunlimitednet.DdunlimitednetIndexer
import amarr.indexer.torznab.TorznabFeed
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import org.slf4j.LoggerFactory

class DdunlimitednetIndexerTest : StringSpec({
    val client = mockk<DdunlimitednetClient>()
    val logger = LoggerFactory.getLogger(this::class.java)
    val sampleLink =
        "ed2k://|file|Dj%20Matrix%20&%20Matt%20Joe%20-%20Musica%20da%20giostra,%20Vol.%2010%20(2023).rar|152488462|0320C47B3BAA01F8D5F42CD7C05CE28D|h=O74TQQWUVF24E7WD25UD57Z45GHIDLZZ|/"

    "should convert links to feed items" {
        val tested = DdunlimitednetIndexer(client, logger)
        coEvery { client.search(any(), listOf()) } returns Result.success(listOf(sampleLink))

        val result = tested.search("matrix", 0, 100, listOf())

        result shouldBe TorznabFeed(
            channel = TorznabFeed.Channel(
                response = TorznabFeed.Channel.Response(
                    offset = 0,
                    total = 1
                ),
                item = listOf(
                    TorznabFeed.Channel.Item(
                        title = "Dj Matrix & Matt Joe - Musica da giostra, Vol. 10 (2023).rar",
                        enclosure = TorznabFeed.Channel.Item.Enclosure(
                            url = "magnet:?xt=urn:btih:AMQMI6Z3VIA7RVPUFTL4AXHCRUAAAAAA&dn=Dj%20Matrix%20%26%20Matt%20Joe%20-%20Musica%20da%20giostra%2C%20Vol.%2010%20%282023%29.rar&xl=152488462&tr=http%3A%2F%2Famarr-reserved",
                            length = 0
                        ),
                        attributes = listOf(
                            TorznabFeed.Channel.Item.TorznabAttribute("category", "1"),
                            TorznabFeed.Channel.Item.TorznabAttribute("seeders", "1"),
                            TorznabFeed.Channel.Item.TorznabAttribute("peers", "1"),
                            TorznabFeed.Channel.Item.TorznabAttribute("size", "152488462"),
                        )
                    )
                )
            )
        )
    }

})
