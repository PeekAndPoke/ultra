package de.peekandpoke.funktor.messaging

import de.peekandpoke.funktor.messaging.api.EmailResult

/**
 * Definition of an email sender
 */
interface EmailSender {
    suspend fun send(email: Email): EmailResult
}
