package io.peekandpoke.funktor.messaging.storage

import io.peekandpoke.funktor.messaging.Email
import io.peekandpoke.funktor.messaging.EmailHooks
import io.peekandpoke.funktor.messaging.api.EmailBody
import io.peekandpoke.funktor.messaging.api.EmailResult
import io.peekandpoke.funktor.messaging.api.SentMessageModel
import io.peekandpoke.funktor.messaging.storage.EmailStoring.Companion.storing

class StoringEmailHook(
    private val repo: SentMessagesStorage,
) : EmailHooks.OnAfterSend {

    override suspend operator fun invoke(email: Email, result: EmailResult) {
        when (val storing = email.storing()) {
            null -> {
                // noop
            }

            is EmailStoring.WithoutContent -> {
                repo.storeSentEmail(
                    result = result,
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

            is EmailStoring.WithContent -> {
                repo.storeSentEmail(
                    result = result,
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
