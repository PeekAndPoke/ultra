package io.peekandpoke.ultra.cache

/** `dynamic`, not `Throwable`: JS can throw any value, and `catch (e: Throwable)` rethrows the others. */
internal actual inline fun <R> catchingAll(block: () -> R, onFailure: (Throwable) -> R): R = try {
    block()
} catch (e: dynamic) {
    onFailure(if (e is Throwable) e else ThrownValue(e))
}

/** A JS error (`TypeError`, ...) has no Kotlin class name: its own `name` then. */
internal actual fun Throwable.failureName(): String? = this::class.simpleName ?: asDynamic().name as? String
