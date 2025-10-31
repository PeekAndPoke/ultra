package de.peekandpoke.funktor.messaging.senders

import de.peekandpoke.funktor.messaging.Email
import de.peekandpoke.funktor.messaging.EmailSender
import de.peekandpoke.funktor.messaging.api.EmailResult
import java.security.SecureRandom
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

    private val random = Random(SecureRandom.getInstanceStrong().nextLong())

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
