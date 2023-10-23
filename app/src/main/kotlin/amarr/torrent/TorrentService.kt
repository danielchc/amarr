package amarr.torrent

import amarr.FINISHED_FOLDER
import amarr.MagnetLink
import amarr.torrent.model.Category
import amarr.torrent.model.TorrentInfo
import amarr.torrent.model.TorrentState
import io.ktor.server.plugins.*
import io.ktor.util.logging.*
import jamule.AmuleClient
import jamule.model.AmuleCategory
import jamule.model.AmuleTransferringFile
import jamule.model.DownloadCommand
import jamule.model.FileStatus
import kotlin.random.Random

class TorrentService(private val amuleClient: AmuleClient, private val log: Logger) {

    fun getTorrentInfo(category: String?) = amuleClient
        .getDownloadQueue()
        .getOrThrow()
        .plus(amuleClient.getSharedFiles().getOrThrow())
        .map { dl ->
            if (dl is AmuleTransferringFile)
                TorrentInfo(
                    hash = dl.fileHashHexString!!,
                    name = dl.fileName!!,
                    size = dl.sizeFull!!,
                    total_size = dl.sizeFull!!,
                    save_path = FINISHED_FOLDER,
                    downloaded = dl.sizeDone!!,
                    progress = dl.sizeDone!!.toDouble() / dl.sizeFull!!.toDouble(),
                    priority = dl.downPrio.toInt(),
                    state = if (dl.sourceXferCount > 0) TorrentState.downloading
                    else when (dl.fileStatus) {
                        FileStatus.READY -> TorrentState.metaDL
                        FileStatus.ERROR -> TorrentState.error
                        FileStatus.COMPLETING -> TorrentState.checkingDL
                        FileStatus.COMPLETE -> TorrentState.uploading
                        FileStatus.PAUSED -> TorrentState.pausedDL
                        FileStatus.ALLOCATING -> TorrentState.allocating
                        FileStatus.INSUFFICIENT -> TorrentState.error
                            .also { log.error("Insufficient disk space") }

                        else -> TorrentState.unknown
                    },
                    category = category ?: categoryById(dl.fileCat)?.name ?: "",
                    dlspeed = dl.speed!!,
                    num_seeds = dl.sourceXferCount.toInt(),
                    eta = computeEta(dl.speed!!, dl.sizeFull!!, dl.sizeDone!!),
                )
            else
            // File is already fully downloaded
                TorrentInfo(
                    hash = dl.fileHashHexString!!,
                    name = dl.fileName!!,
                    size = dl.sizeFull!!,
                    total_size = dl.sizeFull!!,
                    save_path = FINISHED_FOLDER,
                    dlspeed = 0,
                    downloaded = dl.sizeFull!!,
                    progress = 1.0,
                    priority = 0,
                    state = TorrentState.uploading,
                    category = category,
                    eta = 0,
                    num_seeds = 0, // Irrelevant
                )
        }

    private fun computeEta(speed: Long, sizeFull: Long, sizeDone: Long): Int {
        val remainingBytes = sizeFull - sizeDone
        return if (speed == 0L) 8640000 else Math.min((remainingBytes / speed).toInt(), 8640000)
    }

    private fun categoryById(category: Long): Category? {
        return amuleClient.getCategories().getOrThrow().firstOrNull { it.id == category }
            ?.let { Category(it.name, it.path) }
    }

    fun getCategories(): Map<String, Category> = amuleClient
        .getCategories()
        .getOrThrow()
        .map { Category(it.name, it.path) }
        .associateBy { it.name }

    fun addCategory(category: String) = amuleClient.createCategory(
        AmuleCategory(Random.nextInt(1, Int.MAX_VALUE).toLong(), category, "")
    )

    fun addTorrent(urls: List<String>?, category: String?, paused: String?) {
        if (urls == null) {
            log.error("No urls provided")
            throw nonAmarrLink("No urls provided")
        }
        urls.forEach { url ->
            val magnetLink = try {
                MagnetLink.fromString(url)
            } catch (e: Exception) {
                throw nonAmarrLink(url)
            }
            if (!magnetLink.isAmarr()) {
                throw nonAmarrLink(url)
            }
            amuleClient.downloadEd2kLink(magnetLink.toEd2kLink())
            amuleClient.getCategories().getOrThrow().first { it.name == category }.let { cat ->
                amuleClient.setFileCategory(magnetLink.hash, cat.id)
            }
        }
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun deleteTorrent(hashes: List<String>, deleteFiles: String?) = hashes.forEach { hash ->
        amuleClient.sendDownloadCommand(hash.hexToByteArray(), DownloadCommand.DELETE)
    }

    @OptIn(ExperimentalStdlibApi::class)
    fun deleteAllTorrents(deleteFiles: String?) = amuleClient.getSharedFiles().getOrThrow().forEach { file ->
        amuleClient.sendDownloadCommand(file.fileHashHexString!!.hexToByteArray(), DownloadCommand.DELETE)
    }


    private fun nonAmarrLink(url: String): Exception {
        log.error(
            "The provided link does not appear to be an Amarr link: {}. " +
                    "Have you configured Radarr/Sonarr's download client priority correctly? See README.md", url
        )
        return NotFoundException("The provided link does not appear to be an Amarr link: $url")
    }

}