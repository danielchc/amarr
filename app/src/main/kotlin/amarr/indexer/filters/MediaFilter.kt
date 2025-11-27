package amarr.indexer.filters

class MediaFilter(private val mediaExtensions: List<String>) {
    fun filter(fileName: String): Boolean {
        return mediaExtensions.any { fileName.endsWith(".${it}") }
    }
}