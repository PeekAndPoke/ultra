package de.peekandpoke.funktor.core.broker.vault

import de.peekandpoke.funktor.core.broker.CouldNotConvertException
import de.peekandpoke.funktor.core.broker.IncomingParamConverter
import de.peekandpoke.funktor.core.broker.OutgoingParamConverter
import de.peekandpoke.ultra.common.datetime.MpAbsoluteDateTime
import de.peekandpoke.ultra.common.datetime.MpInstant
import de.peekandpoke.ultra.common.datetime.MpLocalDate
import de.peekandpoke.ultra.common.reflection.kType
import kotlin.reflect.KClass
import kotlin.reflect.KType

private val supportedClasses = listOf(
    MpAbsoluteDateTime::class,
    MpInstant::class,
    MpLocalDate::class,
)

/**
 * Checks for all types that are supported by [IncomingMpDateTimeConverter] and [OutgoingMpDateTimeConverter]
 */
private fun supportsType(type: KType): Boolean = type.classifier in supportedClasses

/**
 * Incoming param converter for java.time
 */
class IncomingMpDateTimeConverter : IncomingParamConverter {

    override fun canHandle(type: KType): Boolean = supportsType(type)

    suspend fun <T : Any> convert(value: String, cls: KClass<T>): T {
        @Suppress("UNCHECKED_CAST")
        return convert(value, cls.kType().type) as T
    }

    override suspend fun convert(value: String, type: KType): Any {

        return when (type.classifier) {

            MpAbsoluteDateTime::class,
            MpInstant::class,
                -> try {
                MpInstant.parse(value)
            } catch (e: Throwable) {
                throw IllegalArgumentException("Could not parse MpInstant from '$value'", e)
            }

            MpLocalDate::class -> try {
                MpLocalDate.parse(value)
            } catch (e: Throwable) {
                throw IllegalArgumentException("Could not parse MpLocalDate from '$value'", e)
            }

            else -> {
                throw CouldNotConvertException("Could not convert '$value'")
            }
        }
    }
}

/**
 * Outgoing param converter for java.time
 */
class OutgoingMpDateTimeConverter : OutgoingParamConverter {

    override fun canHandle(type: KType): Boolean = supportsType(type)

    override fun convert(value: Any, type: KType): String {

        return when (type.classifier) {
            MpAbsoluteDateTime::class ->
                (value as? MpAbsoluteDateTime)?.toInstant()?.toIsoString() ?: ""

            MpInstant::class ->
                (value as? MpInstant)?.toIsoString() ?: ""

            MpLocalDate::class ->
                (value as? MpLocalDate)?.format("yyyy-MM-dd") ?: ""

            else -> value.toString()
        }
    }
}
