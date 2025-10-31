package de.peekandpoke.funktor.messaging.senders

import de.peekandpoke.funktor.messaging.Email
import de.peekandpoke.funktor.messaging.EmailHooks
import de.peekandpoke.funktor.messaging.EmailSender
import de.peekandpoke.funktor.messaging.api.EmailResult

class HooksEmailSender(
    private val hooks: EmailHooks,
    private val wrapped: EmailSender,
) : EmailSender {
    override suspend fun send(email: Email): EmailResult {
        return wrapped.send(email).also { result ->
            hooks.applyOnAfterSendHooks(email, result)
        }
    }
}
