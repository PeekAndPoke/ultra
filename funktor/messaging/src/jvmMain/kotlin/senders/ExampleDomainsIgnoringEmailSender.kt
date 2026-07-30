package io.peekandpoke.funktor.messaging.senders

import io.peekandpoke.funktor.messaging.Email
import io.peekandpoke.funktor.messaging.EmailSender
import io.peekandpoke.funktor.messaging.api.EmailResult
import kotlin.random.Random

/**
 * Prevents sending emails to example domains like example.com
 *
 * This sender will return a successful [EmailResult] without sending the email,
 * when all receivers are matching the given [domains].
 */
class ExampleDomainsIgnoringEmailSender(
    private val domains: List<String> = defaultDomains,
    private val wrapped: EmailSender,
) : EmailSender {

    companion object {
        val defaultDomains = listOf("example.com")
    }

    // NOT SecureRandom.getInstanceStrong(): this only builds a placeholder message id, and the
    // composed chain is rebuilt per request — on Linux the strong instance is commonly
    // NativePRNGBlocking, which would block a request thread on entropy.
    private val random = Random.Default

    override suspend fun send(email: Email): EmailResult {
        val allReceivers = email.destination.toAddresses
            .plus(email.destination.bccAddresses)
            .plus(email.destination.ccAddresses)

        val shouldSend = allReceivers.any { receiver ->
            domains.none { receiver.endsWith(it) }
        }

        return if (shouldSend) {
            wrapped.send(email)
        } else {
            EmailResult.ofMessageId("ignore-example-domain-${random.nextLong(10000000, 99999999)}")
        }
    }
}
