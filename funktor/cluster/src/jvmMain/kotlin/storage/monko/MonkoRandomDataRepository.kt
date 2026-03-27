package io.peekandpoke.funktor.cluster.storage.monko

import com.mongodb.client.model.IndexOptions
import io.peekandpoke.funktor.cluster.storage.RandomDataStorage
import io.peekandpoke.funktor.cluster.storage.domain.RawRandomData
import io.peekandpoke.funktor.cluster.storage.domain.category
import io.peekandpoke.funktor.cluster.storage.domain.dataId
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
import org.bson.Document
import kotlin.reflect.full.withNullability

class MonkoRandomDataRepository(
    driver: MonkoDriver,
    timestamped: TimestampedHook,
    repoName: String,
) : RandomDataStorage.Adapter.Vault.Repo, MonkoRepository<RawRandomData>(
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
    }

    override suspend fun find(search: String, page: Int, epp: Int): Cursor<Stored<RawRandomData>> = find { r ->
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

    override suspend fun findOneBy(category: String, dataId: String): Stored<RawRandomData>? {
        val found = find { r ->
            filter(
                and(
                    r.category eq category,
                    r.dataId eq dataId,
                )
            )
            limit(1)
        }

        return found.firstOrNull()
    }

    override fun <T> encode(type: TypeRef<T>, raw: Stored<RawRandomData>): T? {
        @Suppress("UNCHECKED_CAST")
        return driver.codec.awake(type.type.withNullability(true), raw.value.data) as? T?
    }
}
