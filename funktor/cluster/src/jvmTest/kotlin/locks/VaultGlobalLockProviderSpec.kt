package io.peekandpoke.funktor.cluster.locks

import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.shouldBe
import io.peekandpoke.funktor.cluster.locks.domain.GlobalLockEntry
import io.peekandpoke.ultra.datetime.Kronos
import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.reflection.TypeRef
import io.peekandpoke.ultra.reflection.kType
import io.peekandpoke.ultra.vault.Cursor
import io.peekandpoke.ultra.vault.New
import io.peekandpoke.ultra.vault.RemoveResult

import io.peekandpoke.ultra.vault.Repository
import io.peekandpoke.ultra.vault.Stored
import kotlinx.coroutines.flow.single
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Suppress("EXPERIMENTAL_API_USAGE")
class VaultGlobalLockProviderSpec : GlobalLockProviderSpecBase() {

    private val testRepo = TestLockRepo()
    private val globalServerId = GlobalServerId.withoutConfig()

    override fun createSubject() = VaultGlobalLocksProvider(
        repository = testRepo,
        serverId = globalServerId,
        retryDelayMs = 10,
    )

    init {
        "An acquire on a key whose existing lock has expired succeeds without waiting for cleanup" {
            val key = "stale-key"
            val now = MpInstant.now()

            testRepo.insert(
                key = key,
                new = GlobalLockEntry(
                    key = key,
                    serverId = "dead-server",
                    created = now.minus(30.minutes),
                    expires = now.minus(15.minutes),
                ),
            )

            val subject = createSubject()
            val start = Kronos.systemUtc.instantNow()

            val result = subject.acquire(key, 2.seconds) { "got it" }.single()

            val elapsed = Kronos.systemUtc.instantNow() - start

            result shouldBe "got it"
            elapsed shouldBeLessThan 500.milliseconds
        }
    }

    private class TestLockRepo : Repository<GlobalLockEntry> {

        private val entries = mutableMapOf<String, Stored<GlobalLockEntry>>()

        override val name: String = "locks"

        override val connection: String = "TEST::locks"

        override val storedType: TypeRef<GlobalLockEntry> = kType()

        override suspend fun findAll(): Cursor<Stored<GlobalLockEntry>> {
            return Cursor.of(entries.values.toList())
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

                when (entries.contains(idOrKey)) {
                    true -> {
                        entries.remove(idOrKey)
                        RemoveResult(count = 1, query = null)
                    }

                    else -> {
                        RemoveResult(count = 0, query = null)
                    }
                }
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
            error("Not yet implemented")
        }
    }
}
