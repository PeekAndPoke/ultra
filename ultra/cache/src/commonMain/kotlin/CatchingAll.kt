package io.peekandpoke.ultra.cache

/**
 * Runs [block] and hands what it throws to [onFailure] - on JS also a thrown value that is no `Error`
 * (`throw 'x'`), which `catch (e: Throwable)` lets through, wrapped in a [ThrownValue].
 */
internal expect inline fun <R> catchingAll(block: () -> R, onFailure: (Throwable) -> R): R

/** Stands in for a thrown JS value that is no `Error`; its class name is what a failure report shows. */
internal class ThrownValue(val value: Any?) : RuntimeException("a thrown value that is no Throwable")

/** The class name a failure report shows for this throwable. */
internal expect fun Throwable.failureName(): String?
