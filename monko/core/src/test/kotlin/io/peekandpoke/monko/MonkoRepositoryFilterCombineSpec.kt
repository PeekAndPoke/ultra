package io.peekandpoke.monko

import com.mongodb.client.model.Filters.eq
import com.mongodb.kotlin.client.coroutine.MongoClient
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.peekandpoke.ultra.reflection.kType
import io.peekandpoke.ultra.vault.Database
import io.peekandpoke.ultra.vault.New
import io.peekandpoke.ultra.vault.NullEntityCache
import io.peekandpoke.ultra.vault.Repository
import io.peekandpoke.ultra.vault.VaultHookScope

/** Repeated `filter()` calls in `find { }` combine with AND against a real MongoDB (ULTRA-00361). Needs MongoDB. */
class MonkoRepositoryFilterCombineSpec : FreeSpec() {

    data class Item(val name: String, val color: String, val size: Int)

    class ItemsRepo(driver: MonkoDriver) :
        MonkoRepository<Item>(
            name = "monko_filter_combine_spec",
            storedType = kType(),
            driver = driver,
            hooks = Repository.Hooks.empty(),
        )

    private val config = MongoDbConfig.forUnitTests

    private fun createRepo(): ItemsRepo {
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

        return ItemsRepo(driver = driver)
    }

    private suspend fun seeded(): ItemsRepo = createRepo().apply {
        removeAll()
        insert(New(_key = "red-small", value = Item(name = "a", color = "red", size = 1)))
        insert(New(_key = "red-big", value = Item(name = "b", color = "red", size = 9)))
        insert(New(_key = "blue-small", value = Item(name = "c", color = "blue", size = 1)))
        insert(New(_key = "blue-big", value = Item(name = "d", color = "blue", size = 9)))
    }

    init {
        "two filter() calls return only the documents matching both" {
            val repo = seeded()

            val found = repo.find {
                filter(eq("color", "red"))
                filter(eq("size", 1))
            }.toList()

            found.map { it._key } shouldContainExactlyInAnyOrder listOf("red-small")
        }

        "the order of the filter() calls does not change the result" {
            val repo = seeded()

            val found = repo.find {
                filter(eq("size", 9))
                filter(eq("color", "blue"))
            }.toList()

            found.map { it._key } shouldContainExactlyInAnyOrder listOf("blue-big")
        }

        "a single filter() call still filters on its own" {
            val repo = seeded()

            val found = repo.find { filter(eq("color", "blue")) }.toList()

            found.map { it._key } shouldContainExactlyInAnyOrder listOf("blue-small", "blue-big")
        }
    }
}
