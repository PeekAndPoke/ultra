package io.peekandpoke.funktor.messaging.storage

import io.peekandpoke.funktor.messaging.Email
import io.peekandpoke.funktor.messaging.EmailHooks
import io.peekandpoke.funktor.messaging.api.EmailBody
import io.peekandpoke.funktor.messaging.api.EmailResult
import io.peekandpoke.funktor.messaging.api.SentMessageModel
import io.peekandpoke.funktor.messaging.storage.EmailStoring.Companion.storing

/** Post-send hook that persists sent emails to [SentMessagesStorage] based on the [EmailStoring] policy. */
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
                        // The SUBJECT goes through the same policy as the body. It used to be stored
                        // verbatim, which was harmless only while every subject was a Kotlin literal.
                        // Templates are app-overridable now, and a template that interpolates its
                        // `activationUrl` into the subject would write a live single-use token into
                        // the sent-messages inspector — and into every log appender, since the debug
                        // hook logs the subject precisely because the body was assumed to be the only
                        // place a token could be. A URL in a subject is never legitimate, so this
                        // costs nothing for well-behaved mail.
                        subject = storing.modifyContent(email.subject),
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
