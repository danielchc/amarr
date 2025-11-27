package amarr.indexer.search
import java.text.Normalizer

class SearchQuery(val q: String, val searchType: SearchType, val season: Int? = null, val episode: Int? = null, ){
    fun getCleanedQuery(): String{
        val withoutAccents = Normalizer.normalize(q, Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}"), "")

        // Step 2: Remove non-alphanumeric and non-whitespace characters
        return withoutAccents.replace(Regex("[^\\w\\s]"), "")
    }
}