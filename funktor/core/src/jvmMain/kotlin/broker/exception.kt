package de.peekandpoke.ktorfx.core.broker

open class ConverterException(message: String, cause: Throwable? = null) : Throwable(message, cause)

/**
 * Thrown when there is no param converter for a specific type
 */
class NoConverterFoundException(message: String) : ConverterException(message)

/**
 * Thrown when a converter encounters an input that it cannot handle
 */
class CouldNotConvertException(message: String, cause: Throwable? = null) : ConverterException(message, cause)

/**
 * Thrown when a routes param object cannot be handled by the outgoing converter
 */
class InvalidRouteParamsException(message: String) : ConverterException(message)
