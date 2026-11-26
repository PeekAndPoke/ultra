package io.peekandpoke.monko

import com.mongodb.kotlin.client.coroutine.MongoClient
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.reflection.kType
import io.peekandpoke.ultra.vault.Database
import io.peekandpoke.ultra.vault.New
import io.peekandpoke.ultra.vault.NullEntityCache
import io.peekandpoke.ultra.vault.Repository
import io.peekandpoke.ultra.vault.Stored
import io.peekandpoke.ultra.vault.VaultHookScope

/** `remove(entity)` fires OnAfterDelete exactly once, whichever reference type it is called through. Needs MongoDB. */
class MonkoRepositoryRemoveHookSpec : FreeSpec() {

    data class Item(val name: String)

    class RecordingDeleteHook : Repository.Hooks.OnAfterDelete<Item> {
        val received = mutableListOf<Stored<*>>()

        override suspend fun <X : Item> onAfterDelete(repo: Repository<Item>, deleted: Stored<X>) {
            received.add(deleted)
        }
    }

    class ItemsRepo(driver: MonkoDriver, hooks: Repository.Hooks<Item>) :
        MonkoRepository<Item>(
            name = "monko_remove_hook_spec",
            storedType = kType(),
            driver = driver,
            hooks = hooks,
        )

    private val config = MongoDbConfig.forUnitTests

    private fun createRepo(hook: RecordingDeleteHook): ItemsRepo {
        val client: MongoClient = config.toMongoClient()

        val driver = MonkoDriver(
            lazyCodec = lazy {
                MonkoCodec(
                    config = getMonkoDefaultSlumberConfig(),
                    database = Database.withNoRepos,
                    entityCache = NullEntityCache,
                )
            },
            lazyClient = lazy { client },
            lazyDatabase = lazy { client.getDatabase(config.database) },
            hookScope = VaultHookScope.Inline(),
        )

        return ItemsRepo(driver = driver, hooks = Repository.Hooks.of(hook))
    }

    init {
        "remove(entity) through a Repository<T> reference fires OnAfterDelete once" {
            val hook = RecordingDeleteHook()
            val repo = createRepo(hook).apply { removeAll() }
            val stored = repo.insert(New(_key = "via-interface", value = Item("a")))

            val asRepository: Repository<Item> = repo
            val result = asRepository.remove(stored)

            result.count shouldBe 1
            repo.findById(stored._id).shouldBeNull()
            hook.received shouldHaveSize 1
            hook.received.single()._key shouldBe "via-interface"
        }

        "remove(entity) on the MonkoRepository itself fires OnAfterDelete once" {
            val hook = RecordingDeleteHook()
            val repo = createRepo(hook).apply { removeAll() }
            val stored = repo.insert(New(_key = "via-monko", value = Item("b")))

            val result = repo.remove(stored)

            result.count shouldBe 1
            repo.findById(stored._id).shouldBeNull()
            hook.received shouldHaveSize 1
            hook.received.single()._key shouldBe "via-monko"
        }

        "remove(idOrKey) deletes without firing OnAfterDelete" {
            val hook = RecordingDeleteHook()
            val repo = createRepo(hook).apply { removeAll() }
            val stored = repo.insert(New(_key = "via-key", value = Item("c")))

            val result = repo.remove(stored._key)

            result.count shouldBe 1
            repo.findById(stored._id).shouldBeNull()
            hook.received shouldHaveSize 0
        }
    }
}
