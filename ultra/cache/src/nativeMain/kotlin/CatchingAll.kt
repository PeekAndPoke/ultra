package io.peekandpoke.ultra.cache

import kotlin.coroutines.cancellation.CancellationException

/** Everything thrown here is a [Throwable]. */
internal actual inline fun <R> catchingAll(block: () -> R, onFailure: (Throwable) -> R): R = try {
    block()
} catch (e: Throwable) {
    onFailure(e)
}

internal actual fun Throwable.failureName(): String? = this::class.simpleName

internal actual fun isCancellation(e: Throwable): Boolean = e is CancellationException
