package io.peekandpoke.funktor.cluster.storage.monko

import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Projections
import io.peekandpoke.funktor.cluster.storage.RandomCacheStorage
import io.peekandpoke.funktor.cluster.storage.domain.RawCacheData
import io.peekandpoke.funktor.cluster.storage.domain.TypedCacheData
import io.peekandpoke.funktor.cluster.storage.domain.category
import io.peekandpoke.funktor.cluster.storage.domain.dataId
import io.peekandpoke.funktor.cluster.storage.domain.expiresAt
import io.peekandpoke.monko.MonkoDriver
import io.peekandpoke.monko.MonkoRepository
import io.peekandpoke.monko.lang.dsl.and
import io.peekandpoke.monko.lang.dsl.eq
import io.peekandpoke.monko.lang.dsl.or
import io.peekandpoke.monko.lang.dsl.regex
import io.peekandpoke.ultra.reflection.TypeRef
import io.peekandpoke.ultra.reflection.kType
import io.peekandpoke.ultra.vault.Cursor
import io.peekandpoke.ultra.vault.Repository.Hooks
import io.peekandpoke.ultra.vault.Stored
import io.peekandpoke.ultra.vault.hooks.TimestampedHook
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.bson.Document
import java.util.concurrent.TimeUnit
import kotlin.reflect.full.withNullability

class MonkoRandomCacheRepository(
    driver: MonkoDriver,
    timestamped: TimestampedHook,
    repoName: String,
) : RandomCacheStorage.Adapter.Vault.Repo, MonkoRepository<RawCacheData>(
    name = repoName,
    storedType = kType(),
    driver = driver,
    hooks = Hooks.of(
        timestamped.onBeforeSave(),
    ),
) {
    override suspend fun ensureIndexes() {
        driver.createIndex(
            collection = name,
            keys = Document(
                mapOf(
                    field { it.category } to 1,
                    field { it.dataId } to 1,
                )
            ),
            options = IndexOptions().unique(true),
        )

        // TTL index on expiresAt
        driver.createIndex(
            collection = name,
            keys = Document(field { it.expiresAt }, 1),
            options = IndexOptions().expireAfter(0, TimeUnit.SECONDS),
        )
    }

    override suspend fun find(search: String, page: Int, epp: Int): Cursor<Stored<RawCacheData>> = find { r ->
        if (search.isNotBlank()) {
            val pattern = Regex.escape(search.lowercase())
            filter(
                or(
                    r.category regex "(?i)$pattern",
                    r.dataId regex "(?i)$pattern",
                )
            )
        }

        skip(page * epp)
        limit(epp)
    }

    override suspend fun <T> findOneBy(
        key: RandomCacheStorage.CategoryKey<T>,
        dataId: String,
    ): Stored<RawCacheData>? {
        val found = find { r ->
            filter(
                and(
                    r.category eq key.category,
                    r.dataId eq dataId,
                )
            )
            limit(1)
        }

        return found.firstOrNull()
    }

    override suspend fun <T> findOneWithoutData(
        key: RandomCacheStorage.CategoryKey<T>,
        dataId: String,
    ): Stored<RawCacheData>? {
        // Use MongoDB projection to exclude the "data" field
        val coll = driver.database.getCollection<Map<String, Any?>>(name)

        val filter = and(
            repoExpr.category eq key.category,
            repoExpr.dataId eq dataId,
        )

        val result = coll.find(filter)
            .projection(Projections.exclude("data"))
            .limit(1)
            .map { doc ->
                val docKey = "" + doc["_id"]

                @Suppress("UNCHECKED_CAST")
                val value = driver.codec.awake(storedType.type, doc) as RawCacheData

                Stored(
                    _id = "$name/$docKey",
                    _key = docKey,
                    _rev = "",
                    value = value,
                )
            }
            .toList()

        return result.firstOrNull()
    }

    override fun <T> encode(type: TypeRef<T>, raw: RawCacheData): TypedCacheData<T>? {
        @Suppress("UNCHECKED_CAST")
        return (driver.codec.awake(type.type.withNullability(true), raw.data) as? T)?.let { encoded ->
            raw.asTyped { encoded }
        }
    }
}
