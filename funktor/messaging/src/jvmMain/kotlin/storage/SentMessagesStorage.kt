package io.peekandpoke.funktor.messaging.storage

import io.peekandpoke.funktor.messaging.api.EmailAttachment
import io.peekandpoke.funktor.messaging.api.EmailResult
import io.peekandpoke.funktor.messaging.api.SentMessageModel
import io.peekandpoke.ultra.model.PagedSearchFilter
import io.peekandpoke.ultra.vault.Cursor
import io.peekandpoke.ultra.vault.Repository
import io.peekandpoke.ultra.vault.Stored

/** Persistence layer for storing and querying sent messages. */
interface SentMessagesStorage {

    /** No-op implementation that discards all operations. */
    class Null : SentMessagesStorage {

        override suspend fun clear() {
            // noop
        }

        override suspend fun findByRefs(refs: Set<String>, filter: PagedSearchFilter): Cursor<Stored<SentMessage>> {
            return Cursor.empty()
        }

        override suspend fun storeSentEmail(
            result: EmailResult,
            refs: Set<String>,
            tags: Set<String>,
            content: SentMessageModel.Content.EmailContent,
            attachments: List<EmailAttachment>,
        ) {
            // noop
        }
    }

    /** Vault-backed implementation delegating to a [Repo]. */
    class Vault(private val repo: Repo) : SentMessagesStorage {

        /** Low-level repository operations for sent messages. */
        interface Repo : Repository<SentMessage> {
            suspend fun findByRef(refs: Set<String>, filter: PagedSearchFilter): Cursor<Stored<SentMessage>>
        }

        override suspend fun clear() {
            repo.removeAll()
        }

        override suspend fun findByRefs(refs: Set<String>, filter: PagedSearchFilter): Cursor<Stored<SentMessage>> {
            return repo.findByRef(refs, filter)
        }

        override suspend fun storeSentEmail(
            result: EmailResult,
            refs: Set<String>,
            tags: Set<String>,
            content: SentMessageModel.Content.EmailContent,
            attachments: List<EmailAttachment>,
        ) {
            val sent = SentMessage(
                result = result,
                refs = refs,
                tags = tags,
                content = content,
                attachments = attachments,
            )

            repo.insert(sent)
        }
    }

    suspend fun clear()

    suspend fun findByRefs(
        refs: Set<String>,
        filter: PagedSearchFilter = PagedSearchFilter(search = "", page = 1, epp = 50),
    ): Cursor<Stored<SentMessage>>

    suspend fun storeSentEmail(
        result: EmailResult,
        refs: Set<String>,
        tags: Set<String>,
        content: SentMessageModel.Content.EmailContent,
        attachments: List<EmailAttachment>,
    )
}
