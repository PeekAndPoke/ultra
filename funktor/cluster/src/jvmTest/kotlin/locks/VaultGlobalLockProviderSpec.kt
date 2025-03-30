package de.peekandpoke.ktorfx.cluster.locks

import de.peekandpoke.ktorfx.cluster.locks.domain.GlobalLockEntry
import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.common.reflection.kType
import de.peekandpoke.ultra.vault.Cursor
import de.peekandpoke.ultra.vault.New
import de.peekandpoke.ultra.vault.RemoveResult
import de.peekandpoke.ultra.vault.Repository
import de.peekandpoke.ultra.vault.Stored

@Suppress("EXPERIMENTAL_API_USAGE")
class VaultGlobalLockProviderSpec : GlobalLockProviderSpecBase() {

    private val testRepo = TestLockRepo()
    private val globalServerId = GlobalServerId.withoutConfig()

    override fun createSubject() = VaultGlobalLocksProvider(
        repository = testRepo,
        serverId = globalServerId,
        retryDelayMs = 10,
    )

    private class TestLockRepo : Repository<GlobalLockEntry> {

        private val entries = mutableMapOf<String, Stored<GlobalLockEntry>>()

        override val name: String = "locks"

        override val connection: String = "TEST::locks"

        override val storedType: TypeRef<GlobalLockEntry> = kType()

        override suspend fun findAll(): Cursor<Stored<GlobalLockEntry>> {
            return Cursor.of(entries.values)
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
            error("Not yet implemented")
        }

        override suspend fun <X : GlobalLockEntry> save(stored: Stored<X>): Stored<X> {
            error("Not yet implemented")
        }

        override suspend fun removeAll(): RemoveResult {
            error("Not yet implemented")
        }
    }
}

