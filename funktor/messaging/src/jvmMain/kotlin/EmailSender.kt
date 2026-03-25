package io.peekandpoke.funktor.messaging

import io.peekandpoke.funktor.messaging.api.EmailResult

/**
 * Definition of an email sender
 */
interface EmailSender {
    suspend fun send(email: Email): EmailResult
}
