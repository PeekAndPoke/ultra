package de.peekandpoke.funktor.messaging

import de.peekandpoke.funktor.core.config.AppConfig
import de.peekandpoke.funktor.messaging.api.EmailBody
import de.peekandpoke.funktor.messaging.api.EmailDestination

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

        fun developmentMode(config: AppConfig, toEmail: String) {
            val env = "${config.ktor.application.id} | ${config.ktor.deployment.environment}"

            replaceDestination(toEmail)

            prefixBody(
                """
                    <div style="background-color: #ff0000; color: #ffffff; padding: 10px; font-family: monospace;">
                        DEV $env
                    </div>
                """.trimIndent()
            )

            prefixSubject("[$env]")
        }

        fun replaceDestination(to: String) {
            addOverride(MailingOverride.ReplaceDestination.to(to))
        }

        fun prefixBody(prefix: String) {
            addOverride(MailingOverride.PrefixBody(prefix))
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

interface MailingOverride {
    operator fun invoke(email: Email): Email

    object None : MailingOverride {
        override fun invoke(email: Email): Email = email
    }

    class ReplaceDestination(private val newDestination: EmailDestination) : MailingOverride {

        override operator fun invoke(email: Email): Email = email.copy(destination = newDestination)

        companion object {
            fun to(to: String): ReplaceDestination = ReplaceDestination(EmailDestination.to(to))
        }
    }

    class PrefixBody(private val prefix: String) : MailingOverride {
        override operator fun invoke(email: Email): Email = email.copy(
            body = when (val body = email.body) {
                is EmailBody.Text -> {
                    body.copy(content = prefix + email.body.content)
                }

                is EmailBody.Html -> {
                    body.copy(
                        content = email.body.content.replace(
                            oldValue = "<body>",
                            newValue = "<body><div>$prefix</div>"
                        )
                    )
                }
            }
        )
    }

    class PrefixSubject(val prefix: String) : MailingOverride {
        override fun invoke(email: Email): Email = email.copy(subject = prefix + email.subject)
    }
}
