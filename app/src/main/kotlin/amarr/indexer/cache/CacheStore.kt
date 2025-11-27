package amarr.indexer.cache

import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

class CacheStore(
    private val defaultTtlMillis: Long = 1800_000, // default TTL: 30 minutes
) {

    private data class CacheEntry<T>(val value: T, val expiresAt: Instant?)

    private val cache = ConcurrentHashMap<String, CacheEntry<Any>>()

    /**
     * Put a value into the cache
     */
    fun put(key: String, value: Any) {
        val expiresAt = Instant.now().plusMillis(defaultTtlMillis)
        cache[key] = CacheEntry(value, expiresAt)
    }

    /**
     * Get a value from the cache, or null if not found or expired.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: String): T? {
        val entry = cache[key] ?: return null
        if (entry.expiresAt != null && entry.expiresAt.isBefore(Instant.now())) {
            cache.remove(key)
            return null
        }
        return entry.value as? T
    }

    /**
     * Remove a value from the cache.
     */
    fun remove(key: String) {
        cache.remove(key)
    }

    /**
     * Clean up expired entries. You can run this periodically.
     */
    fun cleanup() {
        val now = Instant.now()
        cache.entries.removeIf { it.value.expiresAt?.isBefore(now) == true }
    }

    /**
     * Clear all entries.
     */
    fun clear() {
        cache.clear()
    }
}