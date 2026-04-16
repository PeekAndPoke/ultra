package io.peekandpoke.funktor.auth

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.peekandpoke.funktor.auth.domain.AuthRecord
import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.vault.Stored
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class SessionStoreCachedSpec : StringSpec({

    "getById serves from inner on miss, then from cache until TTL expires" {
        val inner = CountingInnerStore()
        val clock = AtomicLong(1000L)
        val ttlMs = 30_000L

        val cached = SessionStore.Cached(inner = inner, ttlMs = ttlMs, nowMs = { clock.get() })

        inner.setSessionFor(realm = "r", sessionId = "sid-1", ownerId = "u1")

        // Miss → inner hit, result cached
        val first = cached.getById("r", "sid-1")
        first.shouldNotBeNull()
        inner.getByIdCalls.get() shouldBe 1

        // Inside TTL → cache hit, inner not touched
        clock.set(clock.get() + 15_000)
        cached.getById("r", "sid-1")
        cached.getById("r", "sid-1")
        inner.getByIdCalls.get() shouldBe 1

        // After TTL → re-fetch from inner
        clock.set(clock.get() + ttlMs + 1)
        cached.getById("r", "sid-1")
        inner.getByIdCalls.get() shouldBe 2
    }

    "negative lookups are also cached so a missing session doesn't hammer the DB" {
        val inner = CountingInnerStore()
        val cached = SessionStore.Cached(inner = inner, ttlMs = 30_000)

        // No session configured → inner returns null
        cached.getById("r", "ghost").shouldBeNull()
        cached.getById("r", "ghost").shouldBeNull()
        cached.getById("r", "ghost").shouldBeNull()

        inner.getByIdCalls.get() shouldBe 1
    }

    "revoke invalidates the local cache entry and delegates to inner" {
        val inner = CountingInnerStore()
        val cached = SessionStore.Cached(inner = inner, ttlMs = 30_000)

        inner.setSessionFor(realm = "r", sessionId = "sid-1", ownerId = "u1")

        cached.getById("r", "sid-1").shouldNotBeNull()
        inner.getByIdCalls.get() shouldBe 1

        // Simulate server-side removal then a client revoke() call
        inner.clearSession("r", "sid-1")
        cached.revoke("r", "sid-1")

        // After revoke the cache entry is dropped; next lookup re-queries inner and gets null
        cached.getById("r", "sid-1").shouldBeNull()
        inner.revokeCalls.get() shouldBe 1
        inner.getByIdCalls.get() shouldBe 2
    }

    "revokeAllForUser clears the full local cache since we don't index by ownerId" {
        val inner = CountingInnerStore()
        val cached = SessionStore.Cached(inner = inner, ttlMs = 30_000)

        inner.setSessionFor(realm = "r", sessionId = "a", ownerId = "u1")
        inner.setSessionFor(realm = "r", sessionId = "b", ownerId = "u1")
        inner.setSessionFor(realm = "r", sessionId = "c", ownerId = "u2")

        cached.getById("r", "a").shouldNotBeNull()
        cached.getById("r", "b").shouldNotBeNull()
        cached.getById("r", "c").shouldNotBeNull()
        cached.cacheSize() shouldBe 3

        inner.clearSession("r", "a")
        inner.clearSession("r", "b")
        cached.revokeAllForUser("r", "u1")

        cached.cacheSize() shouldBe 0
        inner.revokeAllCalls.get() shouldBe 1
    }

    "realm is part of the cache key so two realms with the same sessionId don't collide" {
        val inner = CountingInnerStore()
        val cached = SessionStore.Cached(inner = inner, ttlMs = 30_000)

        inner.setSessionFor(realm = "r1", sessionId = "same", ownerId = "u1")
        inner.setSessionFor(realm = "r2", sessionId = "same", ownerId = "u2")

        cached.getById("r1", "same")?.resolve()?.ownerId shouldBe "u1"
        cached.getById("r2", "same")?.resolve()?.ownerId shouldBe "u2"
    }
})

/** Fake [SessionStore] that records call counts and serves in-memory state. */
private class CountingInnerStore : SessionStore {

    val getByIdCalls = AtomicInteger(0)
    val revokeCalls = AtomicInteger(0)
    val revokeAllCalls = AtomicInteger(0)

    private data class Key(val realm: String, val sessionId: String)

    private val sessions = mutableMapOf<Key, Stored<AuthRecord.Session>>()

    fun setSessionFor(realm: String, sessionId: String, ownerId: String) {
        sessions[Key(realm, sessionId)] = Stored(
            _id = "sessions/$sessionId",
            _key = sessionId,
            _rev = "",
            value = AuthRecord.Session(
                realm = realm,
                ownerId = ownerId,
                expiresAt = Long.MAX_VALUE,
                token = sessionId,
                deviceFingerprint = "fp",
                userAgent = null,
                ipAddress = null,
                lastSeenAt = MpInstant.Epoch,
            ),
        )
    }

    fun clearSession(realm: String, sessionId: String) {
        sessions.remove(Key(realm, sessionId))
    }

    override suspend fun create(
        realm: String, ownerId: String, deviceFingerprint: String,
        userAgent: String?, ipAddress: String?, ttl: Duration,
    ): Stored<AuthRecord.Session> = error("not used in these tests")

    override suspend fun getById(realm: String, sessionId: String): Stored<AuthRecord.Session>? {
        getByIdCalls.incrementAndGet()
        return sessions[Key(realm, sessionId)]
    }

    override suspend fun touch(realm: String, sessionId: String, now: MpInstant) = Unit

    override suspend fun listForUser(realm: String, ownerId: String) = emptyList<Stored<AuthRecord.Session>>()

    override suspend fun revoke(realm: String, sessionId: String) {
        revokeCalls.incrementAndGet()
    }

    override suspend fun revokeAllForUser(realm: String, ownerId: String, except: String?) {
        revokeAllCalls.incrementAndGet()
    }
}

@Suppress("unused") // Referenced via TTL test to avoid unused-import warning on Duration helper
private val durationRef: Duration = 30.seconds
