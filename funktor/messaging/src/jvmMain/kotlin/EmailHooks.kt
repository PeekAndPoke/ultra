package de.peekandpoke.funktor.messaging

import de.peekandpoke.funktor.messaging.api.EmailResult
import de.peekandpoke.ultra.logging.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope

class EmailHooks(
    private val onAfterSend: List<OnAfterSend>,
    private val log: Log,
) {
    interface OnAfterSend {
        suspend fun onAfterSend(email: Email, result: EmailResult)
    }

    suspend fun applyOnAfterSendHooks(email: Email, result: EmailResult) {

        supervisorScope {
            onAfterSend.forEach {
                launch(Dispatchers.IO) {
                    try {
                        it.onAfterSend(email, result)
                    } catch (e: Throwable) {
                        log.error("Error on after send EmailHook ${it::class.qualifiedName}\n\n${e.stackTraceToString()}")
                    }
                }
            }
        }
    }
}
