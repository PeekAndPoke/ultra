package io.peekandpoke.kraft.modals

import io.peekandpoke.kraft.KraftApp
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.html.DIV
import kotlin.coroutines.resume

/**
 * Renders a modal dialog inside a DIV, receiving the [ModalsManager.Handle] to answer and close it.
 *
 * Runs on EVERY render of the modal stage: never register handlers in it.
 */
typealias ModalRenderer<RESULT> = DIV.(ModalsManager.Handle<RESULT>) -> Unit

/** Registers a [DefaultModalsManager] in the Kraft application. */
fun KraftApp.Builder.modals() = apply {
    setAttribute(ModalsManager.key, DefaultModalsManager())
}

/**
 * Shows the modal and suspends until it is gone, returning its answer, or `null` if it was dismissed.
 *
 * Cancelling the coroutine closes the modal; its handlers then get what [ModalsManager.Handle.resolve] decided, else
 * `null`. Kraft's `launch` scope is global, so a caller's component unmounting does not cancel it.
 */
suspend fun <RESULT : Any> ModalsManager.showAndAwait(view: ModalRenderer<RESULT>): RESULT? {
    return suspendCancellableCoroutine { continuation ->
        val handle = show(view)

        handle.onResult { result ->
            if (continuation.isActive) {
                continuation.resume(result)
            }
        }

        continuation.invokeOnCancellation { handle.close() }
    }
}
