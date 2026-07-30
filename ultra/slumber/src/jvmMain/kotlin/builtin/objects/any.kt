package io.peekandpoke.ultra.slumber.builtin.objects

import io.peekandpoke.ultra.slumber.Awaker
import io.peekandpoke.ultra.slumber.Slumberer

/**
 * Awaker for [Any] types. Returns the raw data as-is, with no copy — an awoken `Any` field aliases
 * the input document, so mutating one mutates the other.
 */
object AnyAwaker : Awaker {
    override fun awake(data: Any?, context: Awaker.Context) = data
}

/** Slumberer for [Any] types. Re-dispatches through the codec on the value's runtime class. */
object AnySlumberer : Slumberer {
    // TODO(scan): a bare `Any()` value has `Any::class` as its runtime class, which resolves straight
    //   back to this slumberer — infinite recursion, i.e. a StackOverflowError.
    override fun slumber(data: Any?, context: Slumberer.Context) = context.slumber(data)
}
