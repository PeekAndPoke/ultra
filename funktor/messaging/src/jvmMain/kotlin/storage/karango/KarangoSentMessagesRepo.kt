package de.peekandpoke.ktorfx.messaging.storage.karango

import de.peekandpoke.karango.aql.DESC
import de.peekandpoke.karango.aql.FOR
import de.peekandpoke.karango.aql.IN
import de.peekandpoke.karango.aql.RETURN
import de.peekandpoke.karango.aql.any
import de.peekandpoke.karango.aql.aql
import de.peekandpoke.karango.aql.expand
import de.peekandpoke.karango.vault.EntityRepository
import de.peekandpoke.karango.vault.IndexBuilder
import de.peekandpoke.karango.vault.KarangoDriver
import de.peekandpoke.ktorfx.messaging.storage.SentMessage
import de.peekandpoke.ktorfx.messaging.storage.SentMessagesStorage
import de.peekandpoke.ktorfx.messaging.storage.createdAt
import de.peekandpoke.ktorfx.messaging.storage.lookup
import de.peekandpoke.ktorfx.messaging.storage.refs
import de.peekandpoke.ultra.common.model.search.PagedSearchFilter
import de.peekandpoke.ultra.common.reflection.kType
import de.peekandpoke.ultra.vault.Cursor
import de.peekandpoke.ultra.vault.Repository
import de.peekandpoke.ultra.vault.Stored
import de.peekandpoke.ultra.vault.hooks.TimestampedHook
import de.peekandpoke.ultra.vault.slumber.ts

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
    override fun IndexBuilder<SentMessage>.buildIndexes() {
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
                FILTER(refs.map { ref -> ref.aql IN message.lookup.refs }.any)
            } else {
                FILTER(false.aql)
            }

            SORT(message.createdAt.ts.DESC)

            PAGE(page = filter.page, epp = filter.epp)

            RETURN(message)
        }
    }
}
