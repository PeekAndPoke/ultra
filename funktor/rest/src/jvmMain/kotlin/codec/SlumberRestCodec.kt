package io.peekandpoke.funktor.rest.codec

import io.peekandpoke.funktor.core.broker.CouldNotConvertException
import io.peekandpoke.ultra.slumber.Codec
import io.peekandpoke.ultra.slumber.JsonUtil.toJsonElement
import io.peekandpoke.ultra.slumber.JsonUtil.unwrap
import io.peekandpoke.ultra.slumber.SlumberConfig
import io.peekandpoke.ultra.slumber.slumber
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import java.lang.reflect.InvocationTargetException
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * Slumber codec for api routes
 */
class SlumberRestCodec(
    config: SlumberConfig,
) : RestCodec, Codec(config) {

    companion object {
        /**
         * Text layer only. Slumber does every bit of the mapping; this turns its plain tree into JSON
         * and back, which is all the Jackson `ObjectMapper` here ever did.
         *
         * Two Jackson settings did not need carrying over. `FAIL_ON_UNKNOWN_PROPERTIES = false` was
         * moot because the body was always read into a `Map<String, Any?>`, which accepts anything —
         * tolerance for unknown fields comes from Slumber's awakers, not from the parser. And
         * `maxStringLength(50_000_000)`, raised to get past Jackson's 20 MB default, has no kotlinx
         * counterpart: kotlinx imposes no such limit, so the ceiling that needed lifting is gone.
         *
         * **One behaviour DID change, deliberately: non-finite doubles now fail.** kotlinx defaults
         * `allowSpecialFloatingPointValues = false`, so a `NaN` or `Infinity` in a response raises
         * rather than being written out. Jackson emitted a bare `NaN`, which is not valid JSON — the
         * browser's `JSON.parse` rejected it, so the old behaviour was also broken, just at the
         * consumer. Failing here is the better half of a bad pair: it happens at the source, names the
         * field, and produces a server-side stack trace instead of an opaque client-side parse error.
         * A `NaN` reaching a response is a division by zero upstream; fix that rather than this.
         */
        private val json = Json

        private val prettyJson = Json { prettyPrint = true }
    }

    private val stringKType: KType = typeOf<String>()
    private val stringKTypeNullable: KType = typeOf<String?>()

    override fun serialize(content: Any?): String? = render(json, slumber(content))

    override fun serializePretty(content: Any?): String? = render(prettyJson, slumber(content))

    override fun serialize(asType: KType, content: Any?): String? = render(json, slumber(asType, content))

    override fun serializePretty(asType: KType, content: Any?): String? =
        render(prettyJson, slumber(asType, content))

    private fun render(with: Json, slumbered: Any?): String = try {
        with.encodeToString(JsonElement.serializer(), slumbered.toJsonElement())
    } catch (e: SerializationException) {
        // kotlinx's own message for this is "Unexpected special floating-point value NaN", which names
        // neither the field nor the fix. Point at both. The offending paths are listed rather than the
        // whole tree so the message stays bounded, and only non-finite doubles are echoed — never
        // arbitrary values, which could be secrets.
        val offenders = findNonFinite(slumbered).take(10)

        if (offenders.isEmpty()) throw e

        throw SerializationException(
            "Response contains non-finite number(s) at: ${offenders.joinToString()}. " +
                    "JSON has no NaN or Infinity, so these cannot be sent. This is a division by zero " +
                    "or an empty-collection average upstream — fix the computation, or map it to null " +
                    "before returning.",
            e,
        )
    }

    /** Paths of every non-finite double in a slumbered tree; empty when there are none. */
    private fun findNonFinite(node: Any?, path: String = "$"): List<String> = when (node) {
        is Double -> if (node.isFinite()) emptyList() else listOf("$path=$node")
        is Float -> if (node.isFinite()) emptyList() else listOf("$path=$node")
        is Map<*, *> -> node.entries.flatMap { (k, v) -> findNonFinite(v, "$path.$k") }
        is Iterable<*> -> node.flatMapIndexed { i, v -> findNonFinite(v, "$path[$i]") }
        else -> emptyList()
    }

    override fun deserialize(asType: KType, content: Any?): Any? {
        return when (content) {
            null -> null
            is String -> when {
                asType == stringKType || asType == stringKTypeNullable -> content

                else -> {
                    // Was `readValue<Map<String, Any?>>`, which assumed the body is a JSON OBJECT and
                    // threw for a top-level array or scalar. Unwrapping any element is strictly more
                    // permissive: Slumber's awaker for `asType` still decides what is acceptable.
                    awakeBody(asType, json.parseToJsonElement(content).unwrap())
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
