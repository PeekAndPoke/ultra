package de.peekandpoke.ktorfx.messaging.storage

import de.peekandpoke.ktorfx.messaging.Email
import de.peekandpoke.ktorfx.messaging.EmailHooks
import de.peekandpoke.ktorfx.messaging.api.EmailBody
import de.peekandpoke.ktorfx.messaging.api.EmailResult
import de.peekandpoke.ktorfx.messaging.api.SentMessageModel
import de.peekandpoke.ultra.common.TypedAttributes
import de.peekandpoke.ultra.common.TypedKey

class StoringEmailHook(
    private val repo: SentMessagesStorage,
) : EmailHooks.OnAfterSend {

    companion object {
        val attr = TypedKey<Storing>("storing")

        fun TypedAttributes.Builder.storing(storing: Storing) {
            add(attr, storing)
        }
    }

    sealed class Storing {
        data class WithoutContent(
            val refs: Set<String>,
            val tags: Set<String>,
        ) : Storing()

        data class Complete(
            val refs: Set<String>,
            val tags: Set<String>,
            val modifyContent: (String) -> String = { it },
        ) : Storing() {
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

    override suspend fun onAfterSend(email: Email, result: EmailResult) {
        when (val storing = email.attributes[attr]) {
            null -> {
                // noop
            }

            is Storing.WithoutContent -> {
                repo.storeSentEmail(
                    refs = storing.refs,
                    tags = storing.tags,
                    content = SentMessageModel.Content.EmailContent(
                        subject = email.subject,
                        destination = email.destination,
                        source = email.source,
                        body = EmailBody.Text("n/a"),
                        result = result,
                    ),
                    attachments = emptyList(),
                )
            }

            is Storing.Complete -> {
                repo.storeSentEmail(
                    refs = storing.refs,
                    tags = storing.tags,
                    content = SentMessageModel.Content.EmailContent(
                        subject = email.subject,
                        destination = email.destination,
                        source = email.source,
                        body = when (val body = email.body) {
                            is EmailBody.Text -> body.copy(
                                content = storing.modifyContent(body.content)
                            )

                            is EmailBody.Html -> body.copy(
                                content = storing.modifyContent(body.content)
                            )
                        },
                        result = result,
                    ),
                    attachments = email.attachments,
                )
            }
        }
    }
}
