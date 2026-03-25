package de.peekandpoke.ultra.slumber

/** Wraps a [Slumberer] to enforce non-null results. Throws [SlumbererException] if the inner slumberer returns null. */
open class NonNullSlumberer(private val inner: Slumberer) : Slumberer {

    override fun slumber(data: Any?, context: Slumberer.Context): Any {
        return inner.slumber(data, context)
            ?: context.reportNullError(data)
    }
}
