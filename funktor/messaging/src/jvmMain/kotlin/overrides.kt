package io.peekandpoke.funktor.messaging

import io.peekandpoke.funktor.core.config.AppConfig
import io.peekandpoke.funktor.messaging.api.EmailBody
import io.peekandpoke.funktor.messaging.api.EmailDestination
import io.peekandpoke.ultra.common.isEmail

/** Composite [MailingOverride] that applies a chain of overrides in order. */
class MailingOverrides(
    val overrides: List<MailingOverride>,
) : MailingOverride {

    companion object {
        fun build(builder: Builder.() -> Unit): MailingOverrides = Builder().apply(builder).build()
    }

    class Builder {
        private val overrides = mutableListOf<MailingOverride>()

        internal fun build() = MailingOverrides(
            overrides = overrides.toList()
        )

        fun developmentMode(config: AppConfig, devConfig: MailingDevConfig?) {
            if (devConfig == null) return

            devConfig.destination?.takeIf { it.isEmail() }?.let { toEmail ->
                replaceDestination(toEmail)
            }

            val env = "${config.ktor.application.id} | ${config.ktor.deployment.environment}"

            prefixBody(
                htmlPrefix = """
                    <div style="background-color: #ff0000; color: #ffffff; padding: 10px; font-family: monospace;">
                        DEV $env
                    </div>
                """.trimIndent(),
                textPrefix = "*** DEV $env ***\n\n",
            )

            prefixSubject("[$env]")
        }

        fun replaceDestination(to: String) {
            addOverride(MailingOverride.ReplaceDestination.to(to))
        }

        fun prefixBody(htmlPrefix: String, textPrefix: String = htmlPrefix) {
            addOverride(MailingOverride.PrefixBody(htmlPrefix = htmlPrefix, textPrefix = textPrefix))
        }

        fun prefixSubject(prefix: String) {
            addOverride(MailingOverride.PrefixSubject(prefix))
        }

        fun addOverride(override: MailingOverride) {
            overrides.add(override)
        }
    }

    override operator fun invoke(email: Email): Email {
        return overrides.fold(initial = email) { acc, next -> next(acc) }
    }
}

/** Transforms an [Email] before it is sent (e.g. redirect destination, prefix subject). */
interface MailingOverride {
    operator fun invoke(email: Email): Email

    /** No-op override that passes the email through unchanged. */
    object None : MailingOverride {
        override fun invoke(email: Email): Email = email
    }

    /** Replaces the email destination (useful for dev/staging environments). */
    class ReplaceDestination(private val newDestination: EmailDestination) : MailingOverride {

        override operator fun invoke(email: Email): Email = email.copy(destination = newDestination)

        companion object {
            fun to(to: String): ReplaceDestination = ReplaceDestination(EmailDestination.to(to))
        }
    }

    /**
     * Prepends a prefix to the email body (e.g. a dev-mode warning banner).
     *
     * [htmlPrefix] is inserted after the opening `<body` tag whatever attributes it carries — matching
     * the literal `"<body>"` silently did nothing for a template written as
     * `body { style = "margin:0" }`, so the dev banner simply never appeared and a dev-box mail was
     * indistinguishable from a production one.
     *
     * [textPrefix] is used for a text body, because injecting markup there shows the user raw HTML.
     */
    class PrefixBody(
        private val htmlPrefix: String,
        private val textPrefix: String = htmlPrefix,
    ) : MailingOverride {

        companion object {
            private val openingBodyTagRegex = "<body(\\s[^>]*)?>".toRegex(RegexOption.IGNORE_CASE)
        }

        override operator fun invoke(email: Email): Email = email.copy(
            body = when (val body = email.body) {
                is EmailBody.Text -> {
                    body.copy(content = textPrefix + body.content)
                }

                is EmailBody.Html -> {
                    val match = openingBodyTagRegex.find(body.content)

                    body.copy(
                        content = when (match) {
                            // No <body> at all — a fragment template. Prepend, so the banner is never
                            // silently dropped.
                            null -> "<div>$htmlPrefix</div>" + body.content
                            else -> body.content.replaceRange(
                                match.range.last + 1,
                                match.range.last + 1,
                                "<div>$htmlPrefix</div>",
                            )
                        }
                    )
                }
            }
        )
    }

    /** Prepends a prefix to the email subject line. */
    class PrefixSubject(val prefix: String) : MailingOverride {
        override fun invoke(email: Email): Email = email.copy(subject = prefix + email.subject)
    }
}
