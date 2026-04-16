package io.peekandpoke.funktor.auth

import io.peekandpoke.funktor.auth.domain.AuthRecord
import io.peekandpoke.ultra.datetime.Kronos
import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.vault.Stored
import io.peekandpoke.ultra.vault.value
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration

/**
 * Persistence + lookup layer for [AuthRecord.Session] rows. The JWT issued to a client carries
 * the session id as a claim; the auth middleware resolves it via this store on every request,
 * cached in memory.
 *
 * Implementations:
 * - [Vault] — backs onto the existing [AuthRecordStorage].
 * - [Cached] — wraps any [SessionStore] with a TTL cache for the hot lookup path.
 * - [Null] — no-op for tests / unconfigured kontainers.
 */
interface SessionStore {

    suspend fun create(
        realm: String,
        ownerId: String,
        deviceFingerprint: String,
        userAgent: String?,
        ipAddress: String?,
        ttl: Duration,
    ): Stored<AuthRecord.Session>

    /**
     * Resolves a session by its id. Returns null if the id is unknown, expired, or the session
     * belongs to a different realm. Implementations may cache.
     */
    suspend fun getById(realm: String, sessionId: String): Stored<AuthRecord.Session>?

    /**
     * Updates the session's `lastSeenAt` to [now]. Implementations are expected to debounce
     * writes (see `RealmTokenConfig.sessionTouchInterval`).
     *
     * Phase 1 note: [Vault.touch] is intentionally a no-op — persistence of `lastSeenAt` is
     * wired in Phase 2 alongside the auth middleware that calls it per-request. Callers should
     * not rely on `lastSeenAt` being current until then.
     */
    suspend fun touch(realm: String, sessionId: String, now: MpInstant)

    suspend fun listForUser(realm: String, ownerId: String): List<Stored<AuthRecord.Session>>

    suspend fun revoke(realm: String, sessionId: String)

    suspend fun revokeAllForUser(realm: String, ownerId: String, except: String? = null)

    object Null : SessionStore {
        override suspend fun create(
            realm: String, ownerId: String, deviceFingerprint: String,
            userAgent: String?, ipAddress: String?, ttl: Duration,
        ): Stored<AuthRecord.Session> = error("SessionStore not configured")

        override suspend fun getById(realm: String, sessionId: String): Stored<AuthRecord.Session>? = null
        override suspend fun touch(realm: String, sessionId: String, now: MpInstant) = Unit
        override suspend fun listForUser(realm: String, ownerId: String): List<Stored<AuthRecord.Session>> = emptyList()
        override suspend fun revoke(realm: String, sessionId: String) = Unit
        override suspend fun revokeAllForUser(realm: String, ownerId: String, except: String?) = Unit
    }

    /**
     * Vault-backed implementation. Sessions live as polymorphic [AuthRecord] rows in the existing
     * auth records repo; revocation is just `removeById`.
     */
    class Vault(
        private val storage: AuthRecordStorage,
        private val random: AuthRandom,
        private val kronos: Kronos,
        private val tokenByteLength: Int = 256,
    ) : SessionStore {

        override suspend fun create(
            realm: String, ownerId: String, deviceFingerprint: String,
            userAgent: String?, ipAddress: String?, ttl: Duration,
        ): Stored<AuthRecord.Session> {
            val now = kronos.instantNow()
            val token = random.getTokenAsBase64(tokenByteLength)

            return storage.create {
                AuthRecord.Session(
                    realm = realm,
                    ownerId = ownerId,
                    expiresAt = now.plus(ttl).toEpochSeconds(),
                    token = token,
                    deviceFingerprint = deviceFingerprint,
                    userAgent = userAgent,
                    ipAddress = ipAddress,
                    lastSeenAt = now,
                )
            }
        }

        override suspend fun getById(realm: String, sessionId: String): Stored<AuthRecord.Session>? {
            if (sessionId.length !in SESSION_ID_MIN_LEN..SESSION_ID_MAX_LEN) return null
            return storage.findByToken(type = AuthRecord.Session, realm = realm, token = sessionId)
        }

        override suspend fun touch(realm: String, sessionId: String, now: MpInstant) {
            // Intentional no-op for Phase 1 — see interface contract. Debounced write to
            // `lastSeenAt` is wired when the auth middleware lands in Phase 2.
        }

        override suspend fun listForUser(realm: String, ownerId: String): List<Stored<AuthRecord.Session>> {
            return storage.findAllByOwner(type = AuthRecord.Session, realm = realm, owner = ownerId)
        }

        override suspend fun revoke(realm: String, sessionId: String) {
            val found = storage.findByToken(type = AuthRecord.Session, realm = realm, token = sessionId)
                ?: return
            storage.removeById(found._id)
        }

        override suspend fun revokeAllForUser(realm: String, ownerId: String, except: String?) {
            val exceptRowId = if (except != null) {
                storage.findByToken(type = AuthRecord.Session, realm = realm, token = except)?._id
            } else null

            storage.removeAllByOwner(
                type = AuthRecord.Session, realm = realm, owner = ownerId, exceptId = exceptRowId,
            )
        }
    }

    /**
     * Wraps another [SessionStore] with a per-JVM TTL cache for [getById]. Hot path avoids the
     * DB. Revocation invalidates the local cache entry; cross-JVM revocation lags by [ttlMs].
     *
     * Positive and negative lookups are cached separately — negatives use [negativeTtlMs] which
     * defaults to a much shorter window so a mistyped or race-acquired session id doesn't
     * pollute the cache for the full positive TTL.
     */
    class Cached(
        private val inner: SessionStore,
        private val ttlMs: Long,
        private val negativeTtlMs: Long = (ttlMs / 10).coerceAtLeast(1_000L),
        private val nowMs: () -> Long = { System.currentTimeMillis() },
    ) : SessionStore by inner {

        private data class Entry(val value: Stored<AuthRecord.Session>?, val expiresAtMs: Long)

        /**
         * Cache key uses a NUL delimiter so a realm or session-id containing `::` can't be
         * crafted to collide with another (realm, sessionId) pair. Session ids are base64 and
         * don't contain NUL, so this is collision-proof in practice.
         */
        private val cache = ConcurrentHashMap<String, Entry>()

        private fun key(realm: String, sessionId: String) = "$realm\u0000$sessionId"

        override suspend fun getById(realm: String, sessionId: String): Stored<AuthRecord.Session>? {
            val k = key(realm, sessionId)
            val now = nowMs()
            val cached = cache[k]
            if (cached != null && cached.expiresAtMs > now) {
                // Defence-in-depth: a session row may have expired while cached; don't serve it.
                val value = cached.value
                if (value != null && isExpired(value, now)) {
                    cache.remove(k, cached)
                    // Fall through to refetch
                } else {
                    return value
                }
            }

            val fresh = inner.getById(realm, sessionId)
            // Defence-in-depth: even if inner returns a non-null row, reject if already expired.
            val freshValid = fresh?.takeUnless { isExpired(it, now) }

            val entryTtl = if (freshValid != null) ttlMs else negativeTtlMs
            // putIfAbsent avoids a cache stampede overwrite when multiple threads miss on the
            // same key concurrently — the first writer wins; the rest use their own fresh value
            // for this call but don't re-poison the cache.
            cache.putIfAbsent(k, Entry(value = freshValid, expiresAtMs = now + entryTtl))
            return freshValid
        }

        override suspend fun revoke(realm: String, sessionId: String) {
            inner.revoke(realm, sessionId)
            cache.remove(key(realm, sessionId))
        }

        override suspend fun revokeAllForUser(realm: String, ownerId: String, except: String?) {
            inner.revokeAllForUser(realm, ownerId, except)
            // We don't know which session ids belonged to this user without an extra lookup;
            // cheaper to clear the whole local cache than maintain an index.
            cache.clear()
        }

        private fun isExpired(session: Stored<AuthRecord.Session>, nowMs: Long): Boolean {
            val expiresAtSec = session.value.expiresAt ?: return false
            // Compare in seconds to avoid Long overflow when expiresAt is near Long.MAX_VALUE.
            return expiresAtSec <= nowMs / 1000L
        }

        /** Test/debugging helper. */
        internal fun cacheSize(): Int = cache.size
    }

    companion object {
        /**
         * Lower bound guards against a malformed or malicious session id causing a DB lookup
         * with unreasonable input. Real tokens are ~344 base64 chars (256 random bytes).
         */
        const val SESSION_ID_MIN_LEN: Int = 16

        /** Upper bound prevents oversized inputs from wasting DB work or cache memory. */
        const val SESSION_ID_MAX_LEN: Int = 1024

        /**
         * Stable hash of (userAgent + ipAddress) used as a device fingerprint. SHA-256 hex.
         * If a real device-id strategy is needed later, swap this implementation.
         */
        fun computeDeviceFingerprint(userAgent: String?, ipAddress: String?): String {
            val md = MessageDigest.getInstance("SHA-256")
            val raw = "${userAgent.orEmpty()}|${ipAddress.orEmpty()}"
            return md.digest(raw.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it) }
        }
    }
}
