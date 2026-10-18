package io.peekandpoke.ultra.slumber

/**
 * Wraps an [Awaker] to enforce non-null results, throwing [AwakerException] when the inner awaker
 * returns null. Applied by [SlumberModule] to every non-nullable declared type.
 *
 * The exception names a path only on the second pass — [Awaker.Context.Fast], used for the first
 * pass, reports `<unknown>`, and [Codec] re-runs with a tracking context once this has thrown.
 */
// TODO(scan): `open` but with a private `inner` and no subclass anywhere, so the wrapped awaker
//   cannot be recovered - ultra/codegen works around it by asking for the nullable type instead.
open class NonNullAwaker(private val inner: Awaker) : Awaker {

    override fun awake(data: Any?, context: Awaker.Context): Any {
        // TODO(scan): the second argument is dead - reportNullError builds its message from the
        //   receiver and ignores the `context` parameter entirely.
        return inner.awake(data, context)
            ?: context.reportNullError(data, context)
    }
}
