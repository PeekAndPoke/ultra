package de.peekandpoke.ktorfx.messaging

import de.peekandpoke.ktorfx.messaging.api.EmailResult

interface Mailing {
    suspend fun send(email: Email): EmailResult
}

class SimpleMailing(private val sender: EmailSender) : Mailing {

    override suspend fun send(email: Email): EmailResult {
        return sender.send(email)
    }
}
