package de.peekandpoke.funktor.messaging

import de.peekandpoke.funktor.messaging.api.EmailResult

interface Mailing {
    suspend fun send(email: Email): EmailResult
}

class SimpleMailing(private val sender: EmailSender) : Mailing {

    override suspend fun send(email: Email): EmailResult {
        return sender.send(email)
    }
}
