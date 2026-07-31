package io.peekandpoke.funktor.messaging.senders.sendgrid

import com.sendgrid.helpers.mail.Mail
import com.sendgrid.helpers.mail.objects.Attachments
import com.sendgrid.helpers.mail.objects.Content
import com.sendgrid.helpers.mail.objects.Email
import com.sendgrid.helpers.mail.objects.Personalization
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.peekandpoke.funktor.messaging.EmailSender
import io.peekandpoke.funktor.messaging.api.EmailBody
import io.peekandpoke.funktor.messaging.api.EmailResult
import kotlinx.io.IOException
import io.peekandpoke.funktor.messaging.Email as LibEmail

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
            expectSuccess = false // we'll handle status codes ourselves
        }

        /**
         * Non-blocking mail send. Returns true if SendGrid accepted the request (HTTP 202).
         */
        suspend fun send(mail: Mail, idempotencyKey: String? = null): EmailResult {
            val url = "$baseUrl/v3/mail/send"

            val response: HttpResponse = http.post(url) {
                header(HttpHeaders.Authorization, "Bearer $apiKey")
                contentType(ContentType.Application.Json)
                idempotencyKey?.let {
                    header("Idempotency-Key", idempotencyKey)
                }
                // sendgrid-java serializes its own Mail — no content-negotiation plugin needed.
                setBody(mail.build())
            }

            @Suppress("UastIncorrectHttpHeaderInspection")
            val messageId = response.headers["X-Message-Id"] ?: response.headers["Message-Id"]

            return if (response.status.isSuccess()) {
                EmailResult.ofMessageId(messageId = messageId ?: "unknown")
            } else {
                val body = try {
                    response.bodyAsText()
                } catch (e: Throwable) {
                    "Response body could not be read:\n${e.stackTraceToString()}"
                }

                EmailResult.error(message = body)
            }
        }
    }

    override suspend fun send(email: LibEmail): EmailResult {
        val from = Email(email.source)

        val personalization = Personalization().apply {
            email.destination.toAddresses.forEach { addTo(Email(it)) }
            email.destination.ccAddresses.forEach { addCc(Email(it)) }
            email.destination.bccAddresses.forEach { addBcc(Email(it)) }
        }

        val subject = email.subject

        val content = when (email.body) {
            is EmailBody.Text -> Content("text/plain", email.body.content)
            is EmailBody.Html -> Content("text/html", email.body.content)
        }

        val attachments = email.attachments.map { attachment ->
            Attachments.Builder(attachment.mimeType, attachment.dataBase64)
        }

        val mail = Mail().apply {
            setFrom(from)
            addPersonalization(personalization)
            setSubject(subject)
            addContent(content)
            attachments.forEach {
                addAttachments(it.build())
            }
        }

        return try {
            client.send(mail = mail, idempotencyKey = email.getIdempotencyKey())
        } catch (e: IOException) {
            EmailResult.ofError(e)
        }
    }
}
