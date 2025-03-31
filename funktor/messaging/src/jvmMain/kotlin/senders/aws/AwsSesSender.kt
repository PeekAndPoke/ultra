package de.peekandpoke.funktor.messaging.senders.aws

import de.peekandpoke.funktor.messaging.Email
import de.peekandpoke.funktor.messaging.EmailSender
import de.peekandpoke.funktor.messaging.api.EmailBody
import de.peekandpoke.funktor.messaging.api.EmailResult
import de.peekandpoke.ultra.common.fromBase64
import kotlinx.coroutines.future.await
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.ses.SesAsyncClient
import software.amazon.awssdk.services.ses.model.Body
import software.amazon.awssdk.services.ses.model.RawMessage
import software.amazon.awssdk.services.ses.model.SendEmailRequest
import software.amazon.awssdk.services.ses.model.SendEmailResponse
import software.amazon.awssdk.services.ses.model.SendRawEmailRequest
import software.amazon.awssdk.services.ses.model.SendRawEmailResponse
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.*
import javax.activation.DataHandler
import javax.activation.DataSource
import javax.mail.Message
import javax.mail.Session
import javax.mail.internet.InternetAddress
import javax.mail.internet.MimeBodyPart
import javax.mail.internet.MimeMessage
import javax.mail.internet.MimeMultipart
import javax.mail.util.ByteArrayDataSource

class AwsSesSender(private val client: SesAsyncClient) : EmailSender {

    companion object {
        fun of(config: AwsSesConfig): AwsSesSender {

            val sesClient = SesAsyncClient.builder()
                .region(
                    Region.of(config.region)
                )
                .credentialsProvider {
                    AwsBasicCredentials.create(config.accessKeyId, config.secretAccessKey)
                }
                .build()

            return AwsSesSender(sesClient)
        }
    }

    override suspend fun send(email: Email): EmailResult {
        return when (email.attachments.isEmpty()) {
            true -> sendWithoutAttachments(email)
            false -> sendWithAttachments(email)
        }
    }

    private suspend fun sendWithoutAttachments(email: Email): EmailResult {
        val request = prepareWithoutAttachments(email) {
            when (email.body) {
                is EmailBody.Html -> html {
                    it.data(email.body.content)
                }

                is EmailBody.Text -> text {
                    it.data(email.body.content)
                }
            }
        }

        return try {
            val sendResult: SendEmailResponse = client.sendEmail(request).await()

            EmailResult.ofMessageId(sendResult.messageId())

        } catch (error: Throwable) {
            EmailResult.ofError(error)
        }
    }

    private fun prepareWithoutAttachments(email: Email, body: Body.Builder.() -> Unit): SendEmailRequest {

        val destination = email.destination

        return SendEmailRequest.builder()
            .source(email.source)
            .destination {
                it.toAddresses(destination.toAddresses)
                    .ccAddresses(destination.ccAddresses)
                    .bccAddresses(destination.bccAddresses)
            }
            .message { msg ->
                msg.subject { it.data(email.subject) }

                msg.body {
                    it.body()
                }
            }
            .build()
    }

    private suspend fun sendWithAttachments(email: Email): EmailResult {

        return try {
            val message = prepareWithAttachments(email)

            val outputStream = ByteArrayOutputStream()
            message.writeTo(outputStream)

            val buf: ByteBuffer = ByteBuffer.wrap(outputStream.toByteArray())

            val arr = ByteArray(buf.remaining())
            buf.get(arr)

            val data: SdkBytes = SdkBytes.fromByteArray(arr)
            val rawMessage: RawMessage = RawMessage.builder().data(data).build()

            val rawEmailRequest: SendRawEmailRequest = SendRawEmailRequest.builder()
                .rawMessage(rawMessage)
                .build()

            val sendResult: SendRawEmailResponse = client.sendRawEmail(rawEmailRequest).await()

            EmailResult.ofMessageId(sendResult.messageId())

        } catch (error: Throwable) {
            EmailResult.ofError(error)
        }
    }

    private fun prepareWithAttachments(email: Email): MimeMessage {
        // see https://docs.aws.amazon.com/code-samples/latest/catalog/javav2-ses-src-main-java-com-example-ses-SendMessageAttachment.java.html

        val session: Session = Session.getDefaultInstance(Properties())

        // Create a new MimeMessage object.
        val message = MimeMessage(session)

        // Add subject, from and to lines.
        message.setSubject(email.subject, "UTF-8")
        message.setFrom(InternetAddress(email.source))

        with(email.destination) {
            message.setRecipients(Message.RecipientType.TO, toAddresses.map { InternetAddress(it) }.toTypedArray())
            message.setRecipients(Message.RecipientType.CC, ccAddresses.map { InternetAddress(it) }.toTypedArray())
            message.setRecipients(Message.RecipientType.BCC, bccAddresses.map { InternetAddress(it) }.toTypedArray())
        }

        // Create a wrapper for the HTML and text parts.
        val wrap = MimeBodyPart()

        // Create a multipart/alternative child container.
        val msgBody = MimeMultipart("alternative")

        val contentPart: MimeBodyPart = when (val body = email.body) {
            is EmailBody.Text -> MimeBodyPart().apply {
                setContent(body.content, "text/plain; charset=UTF-8")
            }

            is EmailBody.Html -> MimeBodyPart().apply {
                setContent(body.content, "text/html; charset=UTF-8")
            }
        }
        // Add content to the msg body part
        msgBody.addBodyPart(contentPart)

        // Add message body to the wrap
        wrap.setContent(msgBody)

        // Create a multipart/mixed parent container.
        val msg = MimeMultipart("mixed")

        // Add the parent container to the message.
        message.setContent(msg)
        msg.addBodyPart(wrap)

        // define the attachment
        email.attachments.forEach { attachment ->
            val att = MimeBodyPart().apply {
                val fds: DataSource = ByteArrayDataSource(
                    attachment.dataBase64.fromBase64(),
                    attachment.mimeType
                )
                dataHandler = DataHandler(fds)
                fileName = attachment.filename
            }

            // Add the attachment to the message.
            msg.addBodyPart(att)
        }

        return message
    }
}
