package de.peekandpoke.ktorfx.rest.codec

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectWriter
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import de.peekandpoke.ultra.common.TypedAttributes
import de.peekandpoke.ultra.slumber.Codec
import de.peekandpoke.ultra.slumber.SlumberConfig
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * Slumber codec for api routes
 */
class SlumberRestCodec(
    config: SlumberConfig,
    attributes: TypedAttributes = TypedAttributes.empty,
    private val jacksonMapper: ObjectMapper = defaultJacksonMapper,
) : RestCodec, Codec(config, attributes) {

    companion object {
        val defaultJacksonMapper: ObjectMapper = ObjectMapper()
            .registerKotlinModule()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)

        val prettyPrinter: ObjectWriter = defaultJacksonMapper.writerWithDefaultPrettyPrinter()
    }

    private val stringKType: KType = typeOf<String>()
    private val stringKTypeNullable: KType = typeOf<String?>()

    override fun serialize(content: Any?): String? {
        val slumbered = slumber(content)

        return jacksonMapper.writeValueAsString(slumbered)
    }

    override fun serializePretty(content: Any?): String? {
        val slumbered = slumber(content)

        return prettyPrinter.writeValueAsString(slumbered)
    }

    override fun serialize(asType: KType, content: Any?): String? {
        val slumbered = slumber(asType, content)

        return jacksonMapper.writeValueAsString(slumbered)
    }

    override fun serializePretty(asType: KType, content: Any?): String? {
        val slumbered = slumber(asType, content)

        return prettyPrinter.writeValueAsString(slumbered)
    }

    override fun deserialize(asType: KType, content: Any?): Any? {
        return when (content) {
            null -> null
            is String -> when {
                asType == stringKType || asType == stringKTypeNullable -> content

                else -> {
                    val json = jacksonMapper.readValue<Map<String, Any?>>(content)

                    awake(asType, json)
                }
            }

            else -> awake(asType, content)
        }
    }
}
