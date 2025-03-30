package de.peekandpoke.ktorfx.messaging.senders

import de.peekandpoke.ktorfx.messaging.Email
import de.peekandpoke.ktorfx.messaging.EmailHooks
import de.peekandpoke.ktorfx.messaging.EmailSender
import de.peekandpoke.ktorfx.messaging.api.EmailResult

class HooksEmailSender(
    private val wrapped: EmailSender,
    private val hooks: EmailHooks,
) : EmailSender {
    override suspend fun send(email: Email): EmailResult {
        return wrapped.send(email).also { result ->
            hooks.applyOnAfterSendHooks(email, result)
        }
    }
}
