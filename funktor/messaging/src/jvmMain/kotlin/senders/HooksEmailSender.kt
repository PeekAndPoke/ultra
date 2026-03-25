package io.peekandpoke.funktor.messaging.senders

import io.peekandpoke.funktor.messaging.Email
import io.peekandpoke.funktor.messaging.EmailHooks
import io.peekandpoke.funktor.messaging.EmailSender
import io.peekandpoke.funktor.messaging.api.EmailResult

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
