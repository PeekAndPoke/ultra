package de.peekandpoke.ktorfx.cluster.storage.karango

import de.peekandpoke.karango.aql.CONTAINS
import de.peekandpoke.karango.aql.EQ
import de.peekandpoke.karango.aql.FOR
import de.peekandpoke.karango.aql.LET
import de.peekandpoke.karango.aql.LOWER
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.aql.UNSET
import de.peekandpoke.karango.aql.any
import de.peekandpoke.karango.aql.aql
import de.peekandpoke.karango.vault.EntityRepository
import de.peekandpoke.karango.vault.IndexBuilder
import de.peekandpoke.karango.vault.KarangoDriver
import de.peekandpoke.ktorfx.cluster.storage.RandomCacheStorage
import de.peekandpoke.ktorfx.cluster.storage.domain.RawCacheData
import de.peekandpoke.ktorfx.cluster.storage.domain.TypedCacheData
import de.peekandpoke.ktorfx.cluster.storage.domain.category
import de.peekandpoke.ktorfx.cluster.storage.domain.dataId
import de.peekandpoke.ktorfx.cluster.storage.domain.expiresAt
import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.common.reflection.kType
import de.peekandpoke.ultra.vault.Cursor
import de.peekandpoke.ultra.vault.Repository.Hooks
import de.peekandpoke.ultra.vault.Stored
import de.peekandpoke.ultra.vault.hooks.TimestampedHook
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
    override fun IndexBuilder<RawCacheData>.buildIndexes() {
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
                    ).any
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
