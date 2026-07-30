package io.peekandpoke.funktor.messaging.storage

import io.peekandpoke.funktor.messaging.Email
import io.peekandpoke.ultra.common.TypedKey

/** Controls how a sent email is persisted: without content, with content, or with anonymized links. */
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
        companion object {
            private const val ANONYMIZED = "#anonymized"

            /** `[^"]*` rather than a lazy `.*?`, which would not match across a newline. */
            private val hrefAttributeRegex = "href=\"[^\"]*\"".toRegex(RegexOption.IGNORE_CASE)

            /**
             * A URL sitting in CONTENT rather than in an attribute.
             *
             * Bounded by whitespace and by the characters that end a URL in markup, so it does not
             * swallow the `</p>` after it. The scheme is OPTIONAL and case-insensitive so that
             * `HTTPS://`, a protocol-relative `//host/path` and a custom `myapp://` deep link are all
             * caught — a token does not become safe by arriving under a different scheme.
             */
            private val bareUrlRegex =
                "(?:[a-z][a-z0-9+.-]*:)?//[^\\s<>\"']+".toRegex(RegexOption.IGNORE_CASE)
        }

        /**
         * Strips every link before the mail is persisted, so a copy kept for support or auditing can
         * never hand out a live token.
         *
         * BOTH passes are needed, and the second is the one that is easy to forget: this policy is
         * applied to `EmailBody.Text` as well as `Html`, and a plain-text mail has no `href` at all —
         * its link is a bare URL. The same is true of an HTML mail that also prints the URL as text
         * for copy-paste, which is a routine thing to do precisely for the mails that carry tokens.
         * Anonymizing only the attribute would have stored those tokens verbatim.
         */
        fun anonymizeLinks() = copy(
            modifyContent = {
                val previous = modifyContent(it)

                hrefAttributeRegex
                    .replace(previous, "href=\"$ANONYMIZED\" onclick=\"return false;\"")
                    .let { withoutHrefs -> bareUrlRegex.replace(withoutHrefs, ANONYMIZED) }
            }
        )
    }
}
