package amarr

import amarr.amule.debugApi
import amarr.indexer.cache.CacheStore
import amarr.category.FileCategoryStore
import amarr.indexer.filters.MediaFilter
import amarr.torrent.torrentApi
import amarr.indexer.implementations.amule.AmuleIndexer
import amarr.indexer.torznab.torznabApi
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import jamule.AmuleClient
import kotlinx.serialization.json.Json
import kotlinx.coroutines.*
import org.jetbrains.annotations.VisibleForTesting
import org.slf4j.Logger
import org.slf4j.event.Level

private const val DEFAULT_AMARR_EXTENSIONS =
    "webm, m4v, 3gp, nsv, ty, strm, rm, rmvb, m3u, ifo, mov, qt, divx, xvid, bivx, nrg, pva, wmv, asf, asx, ogm, ogv, m2v, avi, bin, dat, dvr-ms, mpg, mpeg, mp4, avc, vp3, svq3, nuv, viv, dv, fli, flv, wpl, img, iso, vob, mkv, mk3d, ts, wtv, m2ts, 7z, bz2, gz, r00, rar, tar.bz2, tar.gz, tar, tb2, tbz2, tgz, zip, zipx"


private val AMULE_PORT = System.getenv("AMULE_PORT").apply {
    if (this == null) throw Exception("AMULE_PORT is not set")
}
private val AMULE_HOST = System.getenv("AMULE_HOST").apply {
    if (this == null) throw Exception("AMULE_HOST is not set")
}
private val AMULE_PASSWORD = System.getenv("AMULE_PASSWORD").apply {
    if (this == null) throw Exception("AMULE_PASSWORD is not set")
}
private val AMULE_FINISHED_PATH = System.getenv("AMULE_FINISHED_PATH").let { it ?: "/finished" }



private val AMARR_CONFIG_PATH = System.getenv("AMARR_CONFIG_PATH").let { it ?: "/config" }
private val AMARR_LOG_LEVEL = System.getenv("AMARR_LOG_LEVEL").let { it ?: "WARN" }
private val AMARR_CACHE_TTL_MS: Long = System.getenv("AMARR_CACHE_TTL_MS")?.toLongOrNull() ?: 1800_000
private val AMARR_EXTENSION_FILTER: List<String> = (System.getenv("AMARR_CONFIG_PATH") ?: DEFAULT_AMARR_EXTENSIONS)
    .split(",")
    .map { it.trim() }
    .filter { it.isNotEmpty() }


private fun setLogLevel(logger: Logger) {
    val logBackLogger = logger as ch.qos.logback.classic.Logger
    when (AMARR_LOG_LEVEL) {
        "DEBUG" -> logBackLogger.level = ch.qos.logback.classic.Level.DEBUG
        "INFO" -> logBackLogger.level = ch.qos.logback.classic.Level.INFO
        "WARN" -> logBackLogger.level = ch.qos.logback.classic.Level.WARN
        "ERROR" -> logBackLogger.level = ch.qos.logback.classic.Level.ERROR
        else -> throw Exception("Unknown log level: $AMARR_LOG_LEVEL")
    }
}


fun main() {
    embeddedServer(
        Netty,
        port = 4713
    ) {
        app()
    }.start(wait = true)
}

@VisibleForTesting
internal fun Application.app() {
    setLogLevel(log)
    val cacheStore = CacheStore(AMARR_CACHE_TTL_MS)
    val amuleClient = AmuleClient(AMULE_HOST, AMULE_PORT.toInt(), AMULE_PASSWORD, logger = log)
    val amuleIndexer = AmuleIndexer(amuleClient, log, cacheStore)
    val categoryStore = FileCategoryStore(AMARR_CONFIG_PATH)
    val mediaFilter = MediaFilter(AMARR_EXTENSION_FILTER)

    install(CallLogging) {
        level = Level.DEBUG
    }
    install(ContentNegotiation) {
        json(Json {
            ignoreUnknownKeys = true
            isLenient = true
            prettyPrint = true
            encodeDefaults = true
        })
    }
    debugApi(amuleClient)
    torznabApi(amuleIndexer, mediaFilter)
    torrentApi(amuleClient, categoryStore, AMULE_FINISHED_PATH)
    startPeriodicJob(everyMillis = 60_000) {
        log.debug("Cleaning cached results...")
        cacheStore.cleanup()
    }

}


fun Application.startPeriodicJob(everyMillis: Long, task: suspend () -> Unit) {
    environment.monitor.subscribe(ApplicationStarted) {
        launch {
            while (isActive) {
                try {
                    task()
                } catch (e: Exception) {
                    log.error("Error in periodic task", e)
                }
                delay(everyMillis)
            }
        }
    }
}

