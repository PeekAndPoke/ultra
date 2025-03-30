package de.peekandpoke.ktorfx.messaging.senders

import de.peekandpoke.ktorfx.messaging.Email
import de.peekandpoke.ktorfx.messaging.EmailSender
import de.peekandpoke.ktorfx.messaging.api.EmailResult

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
