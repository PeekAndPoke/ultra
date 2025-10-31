package de.peekandpoke.funktor.messaging.storage

import de.peekandpoke.funktor.messaging.Email
import de.peekandpoke.ultra.common.TypedKey

sealed class EmailStoring {
    companion object {
        val emailStoringKey = TypedKey<EmailStoring>("storing")

        fun Email.store(storing: EmailStoring) = copy(attributes = attributes.plus(emailStoringKey, storing))

        fun Email.storing(): EmailStoring? = attributes[emailStoringKey]

        fun withoutContent(refs: Set<String> = emptySet(), tags: Set<String> = emptySet()): EmailStoring =
            WithoutContent(refs, tags)

        fun withContent(refs: Set<String> = emptySet(), tags: Set<String> = emptySet()): EmailStoring =
            WithContent(refs, tags)

        fun withAnonymizedContent(refs: Set<String> = emptySet(), tags: Set<String> = emptySet()): EmailStoring =
            WithContent(refs, tags).anonymizeLinks()
    }

    data class WithoutContent(
        val refs: Set<String>,
        val tags: Set<String>,
    ) : EmailStoring()

    data class WithContent(
        val refs: Set<String>,
        val tags: Set<String>,
        val modifyContent: (String) -> String = { it },
    ) : EmailStoring() {
        fun anonymizeLinks() = copy(
            modifyContent = {
                val previous = modifyContent(it)

                val regex = "href=\".*?\"".toRegex()

                regex.replace(
                    input = previous,
                    replacement = "href=\"#anonymized\" onclick=\"return false;\"",
                )
            }
        )
    }
}
