package io.peekandpoke.funktor.cluster.locks

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.peekandpoke.funktor.cluster.locks.domain.GlobalLockEntry
import io.peekandpoke.funktor.cluster.locks.domain.isLegacy
import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.reflection.TypeRef
import io.peekandpoke.ultra.reflection.kType
import io.peekandpoke.ultra.vault.Cursor
import io.peekandpoke.ultra.vault.New
import io.peekandpoke.ultra.vault.RemoveResult
import io.peekandpoke.ultra.vault.Stored
import io.peekandpoke.ultra.vault.value
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

@Suppress("EXPERIMENTAL_API_USAGE")
class VaultGlobalLockProviderSpec : VaultGlobalLocksProviderSpecBase() {

    override val repo: VaultGlobalLocksProvider.Repo = TestLockRepo()

    init {
        // Versions without heartbeats judge a lock by `expires`, 15 minutes after its last beat: a longer ttl would let
        // them take over a lock this version still counts as alive.
        "A ttl of 15 minutes is accepted" {
            shouldNotThrowAny { createSubject(ttl = 15.minutes) }
        }

        "A ttl longer than 15 minutes is rejected" {
            shouldThrow<IllegalArgumentException> { createSubject(ttl = 15.minutes + 1.milliseconds) }
        }
    }

    private class TestLockRepo : VaultGlobalLocksProvider.Repo {

        private val entries = mutableMapOf<String, Stored<GlobalLockEntry>>()

        override val name: String = "locks"

        override val connection: String = "TEST::locks"

        override val storedType: TypeRef<GlobalLockEntry> = kType()

        override suspend fun findAll(): Cursor<Stored<GlobalLockEntry>> {
            return Cursor.of(synchronized(entries) { entries.values.toList() })
        }

        override suspend fun <X : GlobalLockEntry> insert(new: New<X>): Stored<X> {
            val key = new._key

            synchronized(entries) {
                if (entries.contains(key)) {
                    error("Could not get lock '$key'")
                }

                entries[key] = new.asStored.copy(_id = key)
            }

            @Suppress("UNCHECKED_CAST")
            return entries[key] as Stored<X>
        }

        override suspend fun remove(idOrKey: String): RemoveResult {
            return synchronized(entries) {
                RemoveResult(count = if (entries.remove(idOrKey) != null) 1 else 0, query = null)
            }
        }

        override suspend fun findById(id: String?): Stored<GlobalLockEntry>? {
            if (id == null) return null
            return synchronized(entries) { entries[id] }
        }

        override suspend fun <X : GlobalLockEntry> save(stored: Stored<X>): Stored<X> {
            error("Not yet implemented")
        }

        override suspend fun removeAll(): RemoveResult {
            return synchronized(entries) {
                RemoveResult(count = entries.size.toLong(), query = null).also { entries.clear() }
            }
        }

        override suspend fun heartbeat(
            key: String,
            token: String,
            heartbeatAt: MpInstant,
            expiresAt: MpInstant,
        ): Boolean = synchronized(entries) {
            val stored = entries[key]?.takeIf { it.value.token == token } ?: return false

            entries[key] = stored.modify { it.copy(lastHeartbeat = heartbeatAt, expires = expiresAt) }

            true
        }

        override suspend fun removeIfHeld(key: String, token: String): Boolean = synchronized(entries) {
            (entries[key]?.value?.token == token).also { held -> if (held) entries.remove(key) }
        }

        override suspend fun removeIfUnchanged(entry: GlobalLockEntry): Boolean = synchronized(entries) {
            val current = entries[entry.key]?.value ?: return false

            val unchanged = when (entry.isLegacy) {
                true -> current.created == entry.created
                false -> current.token == entry.token && current.lastHeartbeat == entry.lastHeartbeat
            }

            unchanged.also { if (it) entries.remove(entry.key) }
        }
    }
}
