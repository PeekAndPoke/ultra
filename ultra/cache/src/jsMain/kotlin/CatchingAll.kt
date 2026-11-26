package io.peekandpoke.ultra.cache

import kotlin.coroutines.cancellation.CancellationException

/** `dynamic`, not `Throwable`: JS can throw any value, and `catch (e: Throwable)` rethrows the others. */
internal actual inline fun <R> catchingAll(block: () -> R, onFailure: (Throwable) -> R): R = try {
    block()
} catch (e: dynamic) {
    onFailure(asThrowable(e))
}

/**
 * [e] itself when it is a [Throwable], else a [ThrownValue]. The check is an `instanceof`, which runs a
 * `Proxy`'s `getPrototypeOf` trap and throws for a revoked one: a value whose check throws is a
 * [ThrownValue] too, so the catch it reached does not throw again.
 */
internal fun asThrowable(e: dynamic): Throwable = try {
    if (e is Throwable) e else ThrownValue()
} catch (_: dynamic) {
    ThrownValue()
}

/**
 * A JS error (`TypeError`, ...) has no Kotlin class name: its own `name` then. Both reads can run user
 * code (a `Proxy`'s traps, a getter): one that throws gives the fixed name `Throwable`.
 */
internal actual fun Throwable.failureName(): String? = try {
    this::class.simpleName ?: asDynamic().name as? String
} catch (_: dynamic) {
    "Throwable"
}

/**
 * An `instanceof`, which runs a `Proxy`'s `getPrototypeOf` trap: one that answered `Error.prototype`
 * before can throw now. Such a value is no cancellation.
 */
internal actual fun isCancellation(e: Throwable): Boolean = try {
    e is CancellationException
} catch (_: dynamic) {
    false
}
