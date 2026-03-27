package io.peekandpoke.funktor.messaging.storage.karango

import io.peekandpoke.funktor.messaging.storage.SentMessage
import io.peekandpoke.funktor.messaging.storage.SentMessagesStorage
import io.peekandpoke.funktor.messaging.storage.createdAt
import io.peekandpoke.funktor.messaging.storage.lookup
import io.peekandpoke.funktor.messaging.storage.refs
import io.peekandpoke.karango.aql.DESC
import io.peekandpoke.karango.aql.FOR
import io.peekandpoke.karango.aql.IN
import io.peekandpoke.karango.aql.RETURN
import io.peekandpoke.karango.aql.anyOrTrueIfEmpty
import io.peekandpoke.karango.aql.aql
import io.peekandpoke.karango.aql.expand
import io.peekandpoke.karango.aql.ts
import io.peekandpoke.karango.vault.EntityRepository
import io.peekandpoke.karango.vault.KarangoDriver
import io.peekandpoke.karango.vault.KarangoIndexBuilder
import io.peekandpoke.ultra.model.PagedSearchFilter
import io.peekandpoke.ultra.reflection.kType
import io.peekandpoke.ultra.vault.Cursor
import io.peekandpoke.ultra.vault.Repository
import io.peekandpoke.ultra.vault.Stored
import io.peekandpoke.ultra.vault.hooks.TimestampedHook

class KarangoSentMessagesRepo(
    driver: KarangoDriver,
    timestamped: TimestampedHook,
    repoName: String,
) : SentMessagesStorage.Vault.Repo, EntityRepository<SentMessage>(
    name = repoName,
    storedType = kType(),
    driver = driver,
    hooks = Repository.Hooks.of(
        timestamped.onBeforeSave(),
    ),
) {
    override fun KarangoIndexBuilder<SentMessage>.buildIndexes() {
        persistentIndex {
            field { lookup.refs.expand() }
        }

        this.persistentIndex {
            field { createdAt.ts }
        }
    }

    override suspend fun findByRef(refs: Set<String>, filter: PagedSearchFilter): Cursor<Stored<SentMessage>> = find {
//        queryOptions {
//            it.count(true).fullCount(true)
//        }

        FOR(repo) { message ->
            if (refs.isNotEmpty()) {
                FILTER(refs.map { ref -> ref.aql IN message.lookup.refs }.anyOrTrueIfEmpty)
            } else {
                FILTER(false.aql)
            }

            SORT(message.createdAt.ts.DESC)

            PAGE(page = filter.page, epp = filter.epp)

            RETURN(message)
        }
    }
}
