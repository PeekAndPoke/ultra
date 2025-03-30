package de.peekandpoke.ktorfx.core.broker

import de.peekandpoke.ultra.common.reflection.TypeRef
import kotlin.reflect.KType

/**
 * The outgoing converter.
 */
class OutgoingConverter(private val converters: List<OutgoingParamConverter>) {

    /**
     * A lookup from [KType] to [OutgoingParamConverter] or null
     */
    private val lookUp = mutableMapOf<KType, OutgoingParamConverter?>()

    /**
     * Returns 'true' when there is an [OutgoingParamConverter] that can handle the given type
     */
    fun canHandle(type: KType): Boolean = findConverter(type) != null

    /**
     * Converts the given value to string
     */
    fun convert(value: Any, type: TypeRef<*>): String = convert(value, type.type)

    /**
     * Converts the given value to string
     */
    fun convert(value: Any, type: KType): String {

        return findConverter(type)?.convert(value, type)
            ?: throw NoConverterFoundException("There is no outgoing converter the can handle the type '$type'")
    }

    /**
     * Returns an [OutgoingParamConverter] for the given [type] or null when no converter is capable
     */
    private fun findConverter(type: KType) = lookUp.getOrPut(type) {
        converters.firstOrNull { it.canHandle(type) }
    }
}
