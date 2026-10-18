package io.peekandpoke.ultra.cache

import io.peekandpoke.ultra.common.RunSync
import kotlinx.coroutines.CompletableDeferred

/**
 * Memoizes lookups that can legitimately resolve to nothing, so a miss is only paid for once.
 *
 * A key that resolved to null stays resolved to null — use [has] to tell "cached as null" from
 * "not cached". This is what separates it from [FastCache], along with the single-flight guarantee
 * below; [FastCache] may run its producer more than once under contention and offers eviction, which
 * this one deliberately does not. Nothing is ever evicted here, so use it where the key space is
 * bounded or the instance is short-lived.
 *
 * [getOrPutAsync] is single-flight **per key**: concurrent callers for one key share one provider
 * run, and callers for different keys never wait on each other. A provider may therefore safely
 * resolve other keys through the same cache — which is the normal shape for nested reference
 * resolution.
 *
 * [getOrPut] is single-flight per key among blocking callers. The two do not coordinate with each
 * other, so a blocking and a suspending caller racing for the same key can both run their provider.
 */
class NullableCache<K : Any, V : Any> {

    private val lock = Any()

    private val entries = mutableMapOf<K, Any>()

    /** Providers currently running, by key, so concurrent callers join instead of duplicating work. */
    private val inFlight = mutableMapOf<K, CompletableDeferred<Any>>()

    /** Outcome of trying to take responsibility for computing one key. */
    private sealed interface Claim {
        /** Already cached — nothing to compute. */
        class Cached(val encoded: Any) : Claim

        /** Someone else is computing it; wait for them rather than running the provider again. */
        class Join(val deferred: CompletableDeferred<Any>) : Claim

        /** Nobody is computing it; this caller must, and must complete [deferred] either way. */
        class Own(val deferred: CompletableDeferred<Any>) : Claim
    }

    /** Number of cached keys, counting those cached as null. */
    val size: Int get() = RunSync(lock) { entries.size }

    /** Drops every entry. */
    fun clear() {
        // TODO(scan): an in-flight getOrPutAsync re-inserts its key after this returns.
        RunSync(lock) { entries.clear() }
    }

    /** True when [key] has a cached result, including when that result is null. */
    fun has(key: K): Boolean = RunSync(lock) { entries.containsKey(key) }

    /** The value cached for [key], or null when it is absent OR cached as null — see [has]. */
    fun get(key: K): V? = read(key)?.decode()

    /** Caches [value] for [key], nulls included, and returns it. */
    fun put(key: K, value: V?): V? {
        RunSync(lock) { entries[key] = value.encode() }

        return value
    }

    /** Returns the cached result for [key], or computes it with [provider] and caches it. */
    fun getOrPut(key: K, provider: () -> V?): V? {
        read(key)?.let { return it.decode() }

        return RunSync(lock) {
            // Branch on PRESENCE, never on the decoded value: MISSING decodes to null, so an elvis
            // here would re-run the provider for every key already cached as a miss.
            entries[key]?.let { return@RunSync it.decode() }

            // TODO(scan): provider runs under the lock - on native that is the process-wide RunSync spin lock.
            provider().also { entries[key] = it.encode() }
        }
    }

    /**
     * Suspending [getOrPut], for providers that need to await, such as a database round trip.
     *
     * The provider runs while **no** lock is held, so it may resolve other keys through this same
     * cache. A single process-wide [kotlinx.coroutines.sync.Mutex] used to span the provider here;
     * being non-reentrant, any nested resolve deadlocked permanently, and unrelated keys serialised
     * behind each other's round trips.
     */
    suspend fun getOrPutAsync(key: K, provider: suspend () -> V?): V? {
        while (true) {
            when (val claim = claim(key)) {
                is Claim.Cached -> return claim.encoded.decode()

                is Claim.Join -> {
                    // Await outside the lock, then re-claim: if the owner succeeded the value is
                    // cached, and if it failed this caller takes over rather than inheriting an
                    // exception raised on somebody else's call stack.
                    runCatching { claim.deferred.await() }
                }

                is Claim.Own -> {
                    val encoded = try {
                        provider().encode()
                    } catch (e: Throwable) {
                        RunSync(lock) { inFlight.remove(key) }
                        claim.deferred.completeExceptionally(e)
                        throw e
                    }

                    RunSync(lock) {
                        entries[key] = encoded
                        inFlight.remove(key)
                    }

                    claim.deferred.complete(encoded)

                    return encoded.decode()
                }
            }
        }
    }

    /** Decides, under the lock, whether this caller reads, waits, or computes. */
    private fun claim(key: K): Claim = RunSync(lock) {
        entries[key]?.let { return@RunSync Claim.Cached(it) }

        inFlight[key]?.let { return@RunSync Claim.Join(it) }

        Claim.Own(CompletableDeferred<Any>().also { inFlight[key] = it })
    }

    private fun read(key: K): Any? = RunSync(lock) { entries[key] }

    private fun V?.encode(): Any = this ?: MISSING

    @Suppress("UNCHECKED_CAST")
    private fun Any.decode(): V? = if (this === MISSING) null else this as V

    private companion object {
        /** Stands in for a cached null, so an absent key stays distinguishable from a null one. */
        private val MISSING = Any()
    }
}
