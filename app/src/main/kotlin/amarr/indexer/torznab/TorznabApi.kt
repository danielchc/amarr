package amarr.indexer.torznab

import amarr.indexer.search.SearchQuery
import amarr.indexer.search.SearchType
import amarr.indexer.implementations.amule.AmuleIndexer
import amarr.indexer.Indexer
import amarr.indexer.exceptions.ThrottledException
import amarr.indexer.exceptions.UnauthorizedException
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.encodeToString
import nl.adaptivity.xmlutil.XmlDeclMode
import nl.adaptivity.xmlutil.core.XmlVersion
import nl.adaptivity.xmlutil.serialization.XML



fun Application.torznabApi(amuleIndexer: AmuleIndexer) {
    routing {
        // Kept for legacy reasons
        get("/api") {
            call.handleRequests(amuleIndexer)
        }
        get("/indexer/amule/api") {
            call.handleRequests(amuleIndexer)
        }
    }
}

private suspend fun ApplicationCall.handleRequests(indexer: Indexer) {
    application.log.debug("Handling Torznab request")
    val xmlFormat = XML {
        xmlDeclMode = XmlDeclMode.Charset
        xmlVersion = XmlVersion.XML10
    } // This API uses XML instead of JSON
    request.queryParameters["t"]?.let {
        when (it) {
            "caps" -> {
                application.log.debug("Handling caps request")
                respondText(xmlFormat.encodeToString(indexer.capabilities()), contentType = ContentType.Application.Xml)
            }
            "tvsearch" -> performSearch(indexer, xmlFormat, SearchType.TV)
            "movie" -> performSearch(indexer, xmlFormat, SearchType.Movie)
            "search" -> performSearch(indexer, xmlFormat, SearchType.Search)

            else -> throw IllegalArgumentException("Unknown action: $it")
        }
    } ?: throw IllegalArgumentException("Missing action")
}

private suspend fun ApplicationCall.performSearch(indexer: Indexer, xmlFormat: XML, searchType: SearchType) {
    val query = request.queryParameters["q"].orEmpty()
    val ep = request.queryParameters["season"]?.toIntOrNull()?: 0
    val season = request.queryParameters["ep"]?.toIntOrNull()?: 0
    val offset = request.queryParameters["offset"]?.toIntOrNull() ?: 0
    val limit = request.queryParameters["limit"]?.toIntOrNull() ?: 100
    val cat = request.queryParameters["cat"]?.split(",")?.map { cat -> cat.toInt() } ?: emptyList()
    application.log.debug("Handling search request: {}, {}, {}, {}", query, offset, limit, cat)
    try {
        val searchQuery: SearchQuery = when (searchType){
            SearchType.TV ->  SearchQuery(query,searchType,ep,season)
            else -> SearchQuery(query,searchType)
        }
        respondText(
            xmlFormat.encodeToString(indexer.search(searchQuery, offset, limit, cat)),
            contentType = ContentType.Application.Xml
        )
    } catch (e: ThrottledException) {
        application.log.warn("Throttled, returning 403")
        respondText("You are being throttled. Retry in a few minutes.", status = HttpStatusCode.Forbidden)
    } catch (e: UnauthorizedException) {
        application.log.warn("Unauthorized, returning 401")
        respondText("Unauthorized, check your credentials.", status = HttpStatusCode.Unauthorized)
    }
}