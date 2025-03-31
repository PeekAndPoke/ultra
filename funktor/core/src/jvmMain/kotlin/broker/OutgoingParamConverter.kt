package de.peekandpoke.funktor.core.broker

import kotlin.reflect.KType

/**
 * Common interface for outgoing param converters
 */
interface OutgoingParamConverter {
    /**
     * Returns 'true' when the converter can handle the given type
     */
    fun canHandle(type: KType): Boolean

    /**
     * Converts to given value to string
     */
    fun convert(value: Any, type: KType): String
}
