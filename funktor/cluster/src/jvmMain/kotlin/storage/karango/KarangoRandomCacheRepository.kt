package io.peekandpoke.funktor.cluster.storage.karango

import io.peekandpoke.funktor.cluster.storage.RandomCacheStorage
import io.peekandpoke.funktor.cluster.storage.domain.RawCacheData
import io.peekandpoke.funktor.cluster.storage.domain.TypedCacheData
import io.peekandpoke.funktor.cluster.storage.domain.category
import io.peekandpoke.funktor.cluster.storage.domain.dataId
import io.peekandpoke.funktor.cluster.storage.domain.expiresAt
import io.peekandpoke.karango.aql.CONTAINS
import io.peekandpoke.karango.aql.EQ
import io.peekandpoke.karango.aql.FOR
import io.peekandpoke.karango.aql.LET
import io.peekandpoke.karango.aql.LOWER
import io.peekandpoke.karango.aql.RETURN
import io.peekandpoke.karango.aql.UNSET
import io.peekandpoke.karango.aql.anyOrTrueIfEmpty
import io.peekandpoke.karango.aql.aql
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

class KarangoRandomCacheRepository(
    driver: KarangoDriver,
    timestamped: TimestampedHook,
    repoName: String,
) : RandomCacheStorage.Adapter.Vault.Repo, EntityRepository<RawCacheData>(
    name = repoName,
    storedType = kType(),
    driver = driver,
    hooks = Hooks.of(
        timestamped.onBeforeSave(),
    ),
) {
    override fun KarangoIndexBuilder<RawCacheData>.buildIndexes() {
        persistentIndex {
            field { category }
            field { dataId }

            options {
                unique(true)
            }
        }

        ttlIndex {
            field { expiresAt }
        }
    }

    override suspend fun find(search: String, page: Int, epp: Int): Cursor<Stored<RawCacheData>> = find {
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

    override suspend fun <T> findOneBy(
        key: RandomCacheStorage.CategoryKey<T>,
        dataId: String,
    ): Stored<RawCacheData>? = findFirst {
        FOR(repo) { entry ->
            FILTER(entry.category EQ key.category)
            FILTER(entry.dataId EQ dataId)

            RETURN(entry)
        }
    }

    override suspend fun <T> findOneWithoutData(
        key: RandomCacheStorage.CategoryKey<T>,
        dataId: String,
    ): Stored<RawCacheData>? = findFirst {
        FOR(repo) { entry ->
            FILTER(entry.category EQ key.category)
            FILTER(entry.dataId EQ dataId)

            RETURN(
                UNSET(entry, "data".aql)
            )
        }
    }

    override fun <T> encode(type: TypeRef<T>, raw: RawCacheData): TypedCacheData<T>? {
        @Suppress("UNCHECKED_CAST")
        return (driver.codec.awake(type.type.withNullability(true), raw.data) as? T)?.let { encoded ->
            raw.asTyped { encoded }
        }
    }
}
