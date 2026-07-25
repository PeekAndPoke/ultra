package io.peekandpoke.funktor.rest.codec

import com.fasterxml.jackson.core.StreamReadConstraints
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectWriter
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import io.peekandpoke.funktor.core.broker.CouldNotConvertException
import io.peekandpoke.ultra.slumber.Codec
import io.peekandpoke.ultra.slumber.SlumberConfig
import io.peekandpoke.ultra.slumber.slumber
import java.lang.reflect.InvocationTargetException
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * Slumber codec for api routes
 */
class SlumberRestCodec(
    config: SlumberConfig,
    private val jacksonMapper: ObjectMapper = defaultJacksonMapper,
) : RestCodec, Codec(config) {

    companion object {
        val defaultJacksonMapper: ObjectMapper = ObjectMapper()
            .registerKotlinModule()
            .apply {
                factory.setStreamReadConstraints(
                    StreamReadConstraints.builder()
                        .maxStringLength(50_000_000)
                        .build()
                )
            }
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

                    awakeBody(asType, json)
                }
            }

            else -> awakeBody(asType, content)
        }
    }

    /**
     * [awake]s a REQUEST BODY, translating a rejected value into a 400 instead of a 500.
     *
     * A `@JvmInline value class` (or any type with an `init { require(...) }`) is constructed
     * reflectively while awaking, so a violated invariant arrives wrapped in an
     * [InvocationTargetException] — which is neither an `AwakerException` (so slumber's
     * [awake] does not handle it) nor a [CouldNotConvertException] (so the status-page mapping
     * would report it as an internal server error, log it as an internal error, and outside
     * production echo the stack trace back to the caller).
     *
     * The body is CLIENT-supplied, so a rejected value is a client error. This mirrors what
     * `IncomingValueClassConverter` already does on the URI-param path, where the same failure
     * is normalized to a fail-closed 404.
     *
     * Deliberately narrow: only a failure thrown from a CONSTRUCTOR
     * ([InvocationTargetException]) whose cause is an [IllegalArgumentException] /
     * [IllegalStateException] — i.e. a `require`/`check` in an `init` block — is translated.
     * Anything else keeps propagating, so a genuine server-side bug still surfaces as a 500.
     */
    private fun awakeBody(asType: KType, data: Any?): Any? {
        try {
            return awake(asType, data)
        } catch (e: Throwable) {
            val rejected = e.rejectedValueCause() ?: throw e

            throw CouldNotConvertException(
                "Could not convert request body to '$asType': ${rejected.message}", rejected,
            )
        }
    }

    /** Walks the cause chain for a constructor-thrown `require`/`check` failure. */
    private fun Throwable.rejectedValueCause(): Throwable? {
        var current: Throwable? = this

        while (current != null) {
            val cause = current.cause

            if (current is InvocationTargetException &&
                (cause is IllegalArgumentException || cause is IllegalStateException)
            ) {
                return cause
            }

            current = cause.takeIf { it !== current }
        }

        return null
    }
}
