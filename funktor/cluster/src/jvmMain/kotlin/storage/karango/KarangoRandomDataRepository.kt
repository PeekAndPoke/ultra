package io.peekandpoke.funktor.cluster.storage.karango

import io.peekandpoke.funktor.cluster.storage.RandomDataStorage
import io.peekandpoke.funktor.cluster.storage.domain.RawRandomData
import io.peekandpoke.funktor.cluster.storage.domain.category
import io.peekandpoke.funktor.cluster.storage.domain.dataId
import io.peekandpoke.karango.aql.CONTAINS
import io.peekandpoke.karango.aql.EQ
import io.peekandpoke.karango.aql.FOR
import io.peekandpoke.karango.aql.LET
import io.peekandpoke.karango.aql.LOWER
import io.peekandpoke.karango.aql.RETURN
import io.peekandpoke.karango.aql.anyOrTrueIfEmpty
import io.peekandpoke.karango.vault.EntityRepository
import io.peekandpoke.karango.vault.KarangoDriver
import io.peekandpoke.karango.vault.KarangoIndexBuilder
import io.peekandpoke.ultra.reflection.TypeRef
import io.peekandpoke.ultra.reflection.kType
import io.peekandpoke.ultra.vault.Cursor
import io.peekandpoke.ultra.vault.Repository.Hooks
import io.peekandpoke.ultra.vault.Stored
import io.peekandpoke.ultra.vault.hooks.TimestampedHook
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
    override fun KarangoIndexBuilder<RawRandomData>.buildIndexes() {
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
                    ).anyOrTrueIfEmpty
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
