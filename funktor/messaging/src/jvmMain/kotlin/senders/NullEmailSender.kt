package de.peekandpoke.funktor.messaging.senders

import de.peekandpoke.funktor.messaging.Email
import de.peekandpoke.funktor.messaging.EmailSender
import de.peekandpoke.funktor.messaging.api.EmailResult

class NullEmailSender : EmailSender {

    override suspend fun send(email: Email): EmailResult {
        return EmailResult(
            success = true,
            messageId = "unknown",
            attributes = mapOf(
                "handledBy" to "NullEmailSender"
            )
        )
    }
}
