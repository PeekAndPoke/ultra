package io.peekandpoke.funktor.messaging

import io.peekandpoke.funktor.messaging.api.EmailResult
import io.peekandpoke.ultra.log.Log
import io.peekandpoke.ultra.log.NullLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

/** Manages post-send hooks that are invoked asynchronously after an email is sent. */
class EmailHooks(
    private val onAfterSend: List<OnAfterSend>,
    private val log: Log,
) {
    /** Hook invoked after an email is sent (e.g. for logging, storage, analytics). */
    interface OnAfterSend {
        suspend operator fun invoke(email: Email, result: EmailResult)
    }

    private class OnAfterSendImpl(
        private val block: suspend (email: Email, result: EmailResult) -> Unit,
    ) : OnAfterSend {
        override suspend fun invoke(email: Email, result: EmailResult) {
            block(email, result)
        }
    }

    /** DSL builder for assembling [EmailHooks]. */
    class Builder(private val log: Log = NullLog) {
        private val onAfterSend = mutableListOf<OnAfterSend>()

        fun onAfterSend(block: OnAfterSend) {
            onAfterSend.add(block)
        }

        fun onAfterSend(block: suspend (email: Email, result: EmailResult) -> Unit) {
            onAfterSend.add(OnAfterSendImpl(block))
        }

        internal fun build() = EmailHooks(
            onAfterSend = onAfterSend.toList(),
            log = log,
        )
    }

    suspend fun applyOnAfterSendHooks(email: Email, result: EmailResult) {

        supervisorScope {
            onAfterSend.forEach {
                launch(Dispatchers.IO) {
                    try {
                        it.invoke(email, result)
                    } catch (e: Throwable) {
                        log.error("Error on after send EmailHook ${it::class.qualifiedName}\n\n${e.stackTraceToString()}")
                    }
                }
            }
        }
    }
}
