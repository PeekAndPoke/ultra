package de.peekandpoke.ultra.slumber

open class NonNullAwaker(private val inner: Awaker) : Awaker {

    override fun awake(data: Any?, context: Awaker.Context): Any {
        return inner.awake(data, context)
            ?: context.reportNullError()
    }
}
