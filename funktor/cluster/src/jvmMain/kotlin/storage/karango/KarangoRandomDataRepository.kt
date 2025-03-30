package de.peekandpoke.ktorfx.cluster.storage.karango

import de.peekandpoke.karango.aql.CONTAINS
import de.peekandpoke.karango.aql.EQ
import de.peekandpoke.karango.aql.FOR
import de.peekandpoke.karango.aql.LET
import de.peekandpoke.karango.aql.LOWER
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.aql.any
import de.peekandpoke.karango.vault.EntityRepository
import de.peekandpoke.karango.vault.IndexBuilder
import de.peekandpoke.karango.vault.KarangoDriver
import de.peekandpoke.ktorfx.cluster.storage.RandomDataStorage
import de.peekandpoke.ktorfx.cluster.storage.domain.RawRandomData
import de.peekandpoke.ktorfx.cluster.storage.domain.category
import de.peekandpoke.ktorfx.cluster.storage.domain.dataId
import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.common.reflection.kType
import de.peekandpoke.ultra.vault.Cursor
import de.peekandpoke.ultra.vault.Repository.Hooks
import de.peekandpoke.ultra.vault.Stored
import de.peekandpoke.ultra.vault.hooks.TimestampedHook
import kotlin.reflect.full.withNullability

class KarangoRandomDataRepository(
    driver: KarangoDriver,
    timestamped: TimestampedHook,
    repoName: String,
) : RandomDataStorage.Adapter.Vault.Repo, EntityRepository<RawRandomData>(
    name = repoName,
    storedType = kType(),
    driver = driver,
    hooks = Hooks.of(
        timestamped.onBeforeSave(),
    ),
) {
    override fun IndexBuilder<RawRandomData>.buildIndexes() {
        persistentIndex {
            field { category }
            field { dataId }

            options {
                unique(true)
            }
        }
    }

    override suspend fun find(search: String, page: Int, epp: Int): Cursor<Stored<RawRandomData>> = find {
//        queryOptions {
//            it.count(true).fullCount(true)
//        }

        val searchLower = LET("search", search.lowercase())

        FOR(repo) { entry ->
            if (search.isNotBlank()) {
                FILTER(
                    listOf(
                        CONTAINS(LOWER(entry.category), searchLower),
                        CONTAINS(LOWER(entry.dataId), searchLower),
                    ).any
                )
            }

            PAGE(page = page, epp = epp)

            RETURN(entry)
        }
    }

    override suspend fun findOneBy(category: String, dataId: String): Stored<RawRandomData>? = findFirst {
        FOR(repo) { entry ->
            FILTER(entry.category EQ category)
            FILTER(entry.dataId EQ dataId)

            LIMIT(1)

            RETURN(entry)
        }
    }

    override fun <T> encode(type: TypeRef<T>, raw: Stored<RawRandomData>): T? {
        @Suppress("UNCHECKED_CAST")
        return driver.codec.awake(type.type.withNullability(true), raw.value.data) as? T?
    }
}
