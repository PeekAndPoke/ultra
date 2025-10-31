package de.peekandpoke.funktor.messaging.senders.sendgrid

import de.peekandpoke.funktor.messaging.EmailSender
import de.peekandpoke.funktor.messaging.api.EmailBody
import de.peekandpoke.funktor.messaging.api.EmailResult
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.io.IOException
import kotlinx.serialization.json.Json
import de.peekandpoke.funktor.messaging.Email as LibEmail

class SendgridSender(private val client: SendGridClientV3) : EmailSender {

    companion object {
        fun of(config: SendgridConfig): SendgridSender {

            val client = SendGridClientV3(
                apiKey = config.apiKey,
                baseUrl = config.baseUrl
            )

            return SendgridSender(client)
        }
    }

    class SendGridClientV3(
        private val apiKey: String,
        private val baseUrl: String,
    ) {
        private val http = HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
            expectSuccess = false // we'll handle status codes ourselves
        }

        /**
         * Non-blocking mail send. Returns true if SendGrid accepted the request (HTTP 202).
         */
        suspend fun send(mail: Mail, idempotencyKey: String? = null): EmailResult {
            val response: HttpResponse = http.post("$baseUrl/v3/mail/send") {
                header(HttpHeaders.Authorization, "Bearer $apiKey")
                header(HttpHeaders.ContentType, ContentType.Application.Json)
                idempotencyKey?.let {
                    header("Idempotency-Key", idempotencyKey)
                }
                if (idempotencyKey != null)
                    setBody(mail)
            }

            @Suppress("UastIncorrectHttpHeaderInspection")
            val messageId = response.headers["X-Message-Id"] ?: response.headers["Message-Id"]

            return if (response.status.isSuccess()) {
                EmailResult.ofMessageId(
                    messageId = messageId ?: "unknown",
                )
            } else {
                val body = try {
                    response.bodyAsText()
                } finally {
                    // noop
                }

                EmailResult.error(message = body)
            }
        }
    }

    override suspend fun send(email: LibEmail): EmailResult {
        val from = Email(email.source)

        val personalization = Personalization(
            to = email.destination.toAddresses.map { Email(it) },
            cc = email.destination.ccAddresses.map { Email(it) },
            bcc = email.destination.bccAddresses.map { Email(it) },
        )

        val subject = email.subject

        val content = when (email.body) {
            is EmailBody.Text -> Content("text/plain", email.body.content)
            is EmailBody.Html -> Content("text/html", email.body.content)
        }

        val attachments = email.attachments.map { attachment -> Attachment.from(attachment) }

        val mail = Mail(
            from = from,
            personalizations = listOf(personalization),
            subject = subject,
            content = listOf(content),
            attachments = attachments.takeIf { it.isNotEmpty() },
        )

        return try {
            client.send(mail = mail, idempotencyKey = email.getIdempotencyKey())
        } catch (e: IOException) {
            EmailResult.ofError(e)
        }
    }
}
