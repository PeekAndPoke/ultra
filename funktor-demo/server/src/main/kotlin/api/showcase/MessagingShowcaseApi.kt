package io.peekandpoke.funktor.demo.server.api.showcase

import io.peekandpoke.funktor.demo.common.showcase.EmailSenderInfo
import io.peekandpoke.funktor.demo.common.showcase.SendTestEmailResponse
import io.peekandpoke.funktor.demo.common.showcase.SentMessageInfo
import io.peekandpoke.funktor.demo.common.showcase.ShowcaseApiClient
import io.peekandpoke.funktor.messaging.Email
import io.peekandpoke.funktor.messaging.api.EmailBody
import io.peekandpoke.funktor.messaging.api.EmailDestination
import io.peekandpoke.funktor.messaging.api.SentMessageModel
import io.peekandpoke.funktor.messaging.funktorMessaging
import io.peekandpoke.funktor.rest.ApiRoutes
import io.peekandpoke.funktor.rest.docs.codeGen
import io.peekandpoke.funktor.rest.docs.docs
import io.peekandpoke.ultra.remote.ApiResponse
import io.peekandpoke.ultra.vault.map

class MessagingShowcaseApi : ApiRoutes("showcase-messaging") {

    val sendTestEmail = ShowcaseApiClient.PostSendTestEmail.mount {
        docs {
            name = "Send a test email"
        }.codeGen {
            funcName = "sendTestEmail"
        }.authorize {
            isSuperUser()
        }.handle { body ->
            val emailBody = if (body.isHtml) EmailBody.Html(body.body) else EmailBody.Text(body.body)

            val result = funktorMessaging.mailing.send(
                Email(
                    source = "noreply@funktor-demo.io",
                    destination = EmailDestination.to(body.to),
                    subject = body.subject,
                    body = emailBody,
                )
            )

            ApiResponse.ok(
                SendTestEmailResponse(
                    success = result.success,
                    messageId = result.messageId,
                    error = result.error?.get("message"),
                )
            )
        }
    }

    val getSentMessages = ShowcaseApiClient.GetSentMessages.mount {
        docs {
            name = "List sent messages"
        }.codeGen {
            funcName = "getSentMessages"
        }.authorize {
            public()
        }.handle {
            val messages = funktorMessaging.sentMessages.findByRefs(refs = emptySet())

            val result = messages.map { stored ->
                val storedValue = stored.resolve()
                val content = storedValue.content
                val emailContent = content as? SentMessageModel.Content.EmailContent

                SentMessageInfo(
                    id = stored._key,
                    to = emailContent?.destination?.toAddresses?.firstOrNull() ?: "?",
                    subject = emailContent?.subject ?: "?",
                    success = storedValue.result?.success ?: false,
                    sentAt = storedValue.createdAt.toIsoString(),
                )
            }

            ApiResponse.ok(result)
        }
    }

    val getEmailSenderInfo = ShowcaseApiClient.GetEmailSenderInfo.mount {
        docs {
            name = "Get email sender info"
        }.codeGen {
            funcName = "getEmailSenderInfo"
        }.authorize {
            public()
        }.handle {
            val sender = funktorMessaging.mailing

            ApiResponse.ok(
                EmailSenderInfo(
                    type = sender::class.simpleName ?: "?",
                    description = "Configured email sender implementation",
                )
            )
        }
    }
}
