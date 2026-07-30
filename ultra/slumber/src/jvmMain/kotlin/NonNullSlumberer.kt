package io.peekandpoke.ultra.slumber

/**
 * Wraps a [Slumberer] to enforce non-null results, throwing [SlumbererException] when the inner
 * slumberer returns null. Applied by [SlumberModule] to every non-nullable declared type.
 */
// TODO(scan): `open` but with a private `inner` and no subclass anywhere, so the wrapped slumberer
//   cannot be recovered - ultra/codegen works around it by asking for the nullable type instead.
open class NonNullSlumberer(private val inner: Slumberer) : Slumberer {

    // TODO(scan): the reported path is only ever as deep as the callers stepped, and
    //   DataClassSlumberer never steps - so a nested field failure is reported as 'root'.
    override fun slumber(data: Any?, context: Slumberer.Context): Any {
        return inner.slumber(data, context)
            ?: context.reportNullError(data)
    }
}
