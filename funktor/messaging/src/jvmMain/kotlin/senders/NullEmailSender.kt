package io.peekandpoke.funktor.messaging.senders

import io.peekandpoke.funktor.messaging.Email
import io.peekandpoke.funktor.messaging.EmailSender
import io.peekandpoke.funktor.messaging.api.EmailResult

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
