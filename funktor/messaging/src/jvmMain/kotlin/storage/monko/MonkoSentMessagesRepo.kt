package io.peekandpoke.funktor.messaging.storage.monko

import com.mongodb.client.model.Filters
import io.peekandpoke.funktor.messaging.storage.SentMessage
import io.peekandpoke.funktor.messaging.storage.SentMessagesStorage
import io.peekandpoke.funktor.messaging.storage.createdAt
import io.peekandpoke.funktor.messaging.storage.lookup
import io.peekandpoke.funktor.messaging.storage.refs
import io.peekandpoke.monko.MonkoDriver
import io.peekandpoke.monko.MonkoRepository
import io.peekandpoke.monko.lang.dsl.desc
import io.peekandpoke.monko.lang.ts
import io.peekandpoke.ultra.model.PagedSearchFilter
import io.peekandpoke.ultra.reflection.kType
import io.peekandpoke.ultra.vault.Cursor
import io.peekandpoke.ultra.vault.Repository
import io.peekandpoke.ultra.vault.Stored
import io.peekandpoke.ultra.vault.hooks.TimestampedHook
import org.bson.Document

class MonkoSentMessagesRepo(
    name: String = "system_sent_messages",
    driver: MonkoDriver,
    timestamped: TimestampedHook,
) : SentMessagesStorage.Vault.Repo, MonkoRepository<SentMessage>(
    name = name,
    storedType = kType(),
    driver = driver,
    hooks = Repository.Hooks.of(
        timestamped.onBeforeSave(),
    ),
) {
    override suspend fun ensureIndexes() {
        driver.createIndex(
            collection = name,
            keys = Document(
                mapOf(
                    field { it.lookup.refs } to 1,
                )
            ),
        )

        driver.createIndex(
            collection = name,
            keys = Document(
                mapOf(
                    field { it.createdAt.ts } to -1,
                )
            ),
        )
    }

    override suspend fun findByRef(refs: Set<String>, filter: PagedSearchFilter): Cursor<Stored<SentMessage>> {
        if (refs.isEmpty()) {
            return Cursor.empty()
        }

        return find { r ->
            filter(Filters.`in`(field { it.lookup.refs }, refs))
            sort(r.createdAt.ts.desc)
            skip(filter.page * filter.epp)
            limit(filter.epp)
        }
    }
}
