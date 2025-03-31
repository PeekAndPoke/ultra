package de.peekandpoke.funktor.messaging.senders

import de.peekandpoke.funktor.messaging.Email
import de.peekandpoke.funktor.messaging.EmailSender
import de.peekandpoke.funktor.messaging.MailingOverride
import de.peekandpoke.funktor.messaging.api.EmailResult

class OverridingEmailSender(
    private val override: MailingOverride,
    private val wrapped: EmailSender,
) : EmailSender {

    override suspend fun send(email: Email): EmailResult {

        val overridden = override(email)

        return wrapped.send(overridden)
            .withAttributes("sender" to email.source)
            .withAttributes("destination" to email.destination)
            .let {
                if (email.destination != overridden.destination) {
                    it.withAttributes("destinationOverride" to overridden.destination)
                } else {
                    it
                }
            }
            .withAttributes("subject" to email.subject)
            .let {
                if (email.subject != overridden.subject) {
                    it.withAttributes("subjectOverride" to overridden.subject)
                } else {
                    it
                }
            }
    }
}
