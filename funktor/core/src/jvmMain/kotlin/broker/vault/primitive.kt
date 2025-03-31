package de.peekandpoke.funktor.core.broker.vault

import de.peekandpoke.funktor.core.broker.IncomingParamConverter
import de.peekandpoke.funktor.core.broker.NoConverterFoundException
import de.peekandpoke.funktor.core.broker.OutgoingParamConverter
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.reflect.KClass
import kotlin.reflect.KType

/**
 * Checks for all types supported [IncomingPrimitiveConverter] and [OutgoingPrimitiveConverter]
 */
private fun supportsType(type: KType): Boolean = type.classifier.let { cls ->
    cls == Int::class ||
            cls == Float::class ||
            cls == Double::class ||
            cls == Long::class ||
            cls == Boolean::class ||
            cls == String::class ||
            cls == BigDecimal::class ||
            cls == BigInteger::class ||
            (cls is KClass<*> && cls.java.isEnum)
}

/**
 * Incoming param converter for primitive types
 */
class IncomingPrimitiveConverter : IncomingParamConverter {

    override fun canHandle(type: KType): Boolean = supportsType(type)

    override suspend fun convert(value: String, type: KType): Any = when (type.classifier) {

        Int::class, java.lang.Integer::class -> value.toInt()

        Float::class, java.lang.Float::class -> value.toFloat()

        Double::class, java.lang.Double::class -> value.toDouble()

        Long::class, java.lang.Long::class -> value.toLong()

        Boolean::class, java.lang.Boolean::class -> value.toBoolean()

        String::class, java.lang.String::class -> value

        BigDecimal::class -> BigDecimal(value)

        BigInteger::class -> BigInteger(value)

        // otherwise it must be an enum
        else -> (type.classifier as KClass<*>).java.enumConstants?.firstOrNull { (it as Enum<*>).name == value }
            ?: throw NoConverterFoundException("Value '$value' is not a enum member name of '$type'")
    }
}

/**
 * Outgoing param converter for primitive types
 */
class OutgoingPrimitiveConverter : OutgoingParamConverter {

    override fun canHandle(type: KType): Boolean = supportsType(type)

    override fun convert(value: Any, type: KType): String = value.toString()
}
