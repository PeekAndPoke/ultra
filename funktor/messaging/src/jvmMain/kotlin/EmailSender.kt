package de.peekandpoke.ktorfx.messaging

import de.peekandpoke.ktorfx.messaging.api.EmailResult

/**
 * Definition of an email sender
 */
interface EmailSender {
    suspend fun send(email: Email): EmailResult
}
