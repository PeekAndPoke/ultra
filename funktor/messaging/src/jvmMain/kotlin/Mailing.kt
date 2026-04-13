package io.peekandpoke.funktor.messaging

import io.peekandpoke.funktor.messaging.api.EmailResult

/** High-level interface for sending emails, potentially with overrides or hooks applied. */
interface Mailing {
    suspend fun send(email: Email): EmailResult
}

/** Straightforward [Mailing] that delegates directly to an [EmailSender]. */
class SimpleMailing(private val sender: EmailSender) : Mailing {

    override suspend fun send(email: Email): EmailResult {
        return sender.send(email)
    }
}
