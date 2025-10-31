package de.peekandpoke.funktor.messaging

import de.peekandpoke.funktor.messaging.api.EmailResult
import de.peekandpoke.ultra.log.Log
import de.peekandpoke.ultra.log.NullLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

class EmailHooks(
    private val onAfterSend: List<OnAfterSend>,
    private val log: Log,
) {
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
