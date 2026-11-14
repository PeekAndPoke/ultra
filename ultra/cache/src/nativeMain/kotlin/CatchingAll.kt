package io.peekandpoke.ultra.cache

/** Everything thrown here is a [Throwable]. */
internal actual inline fun <R> catchingAll(block: () -> R, onFailure: (Throwable) -> R): R = try {
    block()
} catch (e: Throwable) {
    onFailure(e)
}

internal actual fun Throwable.failureName(): String? = this::class.simpleName
