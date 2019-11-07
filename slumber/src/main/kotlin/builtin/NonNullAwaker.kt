package de.peekandpoke.ultra.slumber.builtin

import de.peekandpoke.ultra.slumber.Awaker

open class NonNullAwaker(private val inner: Awaker) : Awaker {

    override fun awake(data: Any?, context: Awaker.Context): Any {
        return inner.awake(data, context)
            ?: context.reportNullError()
    }
}
