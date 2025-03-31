package de.peekandpoke.funktor.core.broker

import kotlin.reflect.KType

/**
 * Common interface for incoming param converters
 */
interface IncomingParamConverter {
    /**
     * Returns 'true' when the converter can handle the given type
     */
    fun canHandle(type: KType): Boolean

    /**
     * Converts to given String to an appropriate value
     */
    suspend fun convert(value: String, type: KType): Any?
}
