package io.peekandpoke.funktor.messaging

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.peekandpoke.funktor.messaging.api.EmailAttachment
import io.peekandpoke.funktor.messaging.api.EmailBody
import io.peekandpoke.funktor.messaging.api.EmailDestination
import io.peekandpoke.funktor.messaging.senders.aws.AwsSesSender
import io.peekandpoke.ultra.common.toBase64
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.ses.SesAsyncClient
import java.io.ByteArrayOutputStream

/**
 * Covers the jakarta.mail wiring of the SES sender.
 *
 * The library moved from the `javax.mail` namespace to `jakarta.mail` with the 2.x bump, and the
 * only other test in this area asserts the sender is never CONSTRUCTED in test mode — so nothing
 * exercised the MIME building itself. Building a message needs no AWS call, only a client instance.
 */
class AwsSesMimeSpec : StringSpec({

    // region and credentials are required to CONSTRUCT the client; building a MIME message makes
    // no call, so dummy values are enough
    fun sender() = AwsSesSender(
        client = SesAsyncClient.builder()
            .region(Region.US_EAST_1)
            .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("k", "s")))
            .build()
    )

    fun emailWith(vararg attachments: EmailAttachment) = Email(
        source = "sender@corp.test",
        destination = EmailDestination.to("user@corp.test"),
        subject = "A subject",
        body = EmailBody.Text("hello"),
        attachments = attachments.toList(),
    )

    "builds a multipart message carrying the attachment" {
        val attachment = EmailAttachment(
            mimeType = "text/plain",
            filename = "notes.txt",
            dataBase64 = "hello world".toBase64(),
        )

        val message = sender().prepareWithAttachments(emailWith(attachment))

        message.subject shouldBe "A subject"

        val rendered = ByteArrayOutputStream().also { message.writeTo(it) }.toString()

        rendered shouldContain "notes.txt"
        rendered shouldContain "multipart/mixed"
        rendered shouldContain "user@corp.test"
    }

    "names the attachment when its payload is not valid base64" {
        val broken = EmailAttachment(
            mimeType = "text/plain",
            filename = "broken.txt",
            dataBase64 = "not base64!",
        )

        val ex = runCatching { sender().prepareWithAttachments(emailWith(broken)) }.exceptionOrNull()

        ex!!.message!! shouldContain "broken.txt"
    }
})
