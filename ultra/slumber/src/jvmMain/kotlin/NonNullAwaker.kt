package io.peekandpoke.ultra.slumber

/** Wraps an [Awaker] to enforce non-null results. Throws [AwakerException] if the inner awaker returns null. */
open class NonNullAwaker(private val inner: Awaker) : Awaker {

    override fun awake(data: Any?, context: Awaker.Context): Any {
        return inner.awake(data, context)
            ?: context.reportNullError(data, context)
    }
}
