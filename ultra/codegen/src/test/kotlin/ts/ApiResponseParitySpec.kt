package io.peekandpoke.ultra.codegen.ts

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.model.Message
import io.peekandpoke.ultra.remote.ApiResponse
import io.peekandpoke.ultra.remote.HttpStatusCode
import io.peekandpoke.ultra.slumber.Codec
import kotlin.reflect.KType
import kotlin.reflect.typeOf

/**
 * Guards the hand-written `runtime/apiResponse.ts` against the envelope it mirrors.
 *
 * Same reasoning as [io.peekandpoke.ultra.codegen.contributors.MpDateTimeFieldParitySpec]: hand-written
 * TypeScript is never checked by the generator, so the only defence against a wrong shape is slumbering
 * a real value and comparing. The envelope is worse than most in this respect — it wraps EVERY endpoint,
 * so a wrong field here breaks the whole SDK rather than one type.
 */
class ApiResponseParitySpec : FreeSpec() {

    private val codec = Codec.default

    private val runtimeTs: String by lazy { readResource("ts/runtime/apiResponse.ts") }

    private val datetimeTs: String by lazy { readResource("ts/runtime/datetime.ts") }

    private fun readResource(path: String): String =
        this::class.java.classLoader.getResourceAsStream(path)?.bufferedReader()?.readText()
            ?: error("$path is not on the classpath")

    /** Slumbers [value] as [type] and returns its keys. */
    private fun slumberedKeys(type: KType, value: Any?): Set<String> {
        val map = codec.slumber(type, value) as? Map<*, *>
            ?: error("expected $type to slumber to a map")

        return map.keys.map { it.toString() }.toSet()
    }

    /**
     * The property names declared at the top level of the first `{ ... }` block following [anchor].
     *
     * Reading them out of the source is what makes this test able to fail: comparing the Kotlin shape
     * against a hand-maintained Kotlin list would only ever prove the list was copied correctly.
     */
    private fun String.keysAfter(anchor: String, openMarker: String = "{"): Set<String> {
        val from = indexOf(anchor)

        require(from >= 0) { "no '$anchor' found" }

        val open = indexOf(openMarker, from)

        require(open >= 0) { "no '$openMarker' after '$anchor'" }

        val start = open + openMarker.length - 1
        var depth = 0
        var end = start

        while (end < length) {
            when (this[end]) {
                '{' -> depth++
                '}' -> if (--depth == 0) break
            }
            end++
        }

        val keys = mutableSetOf<String>()
        var level = 0

        substring(start + 1, end).lineSequence().forEach { line ->
            val trimmed = line.trim()

            if (level == 0) {
                Regex("^(\\w+)\\??:").find(trimmed)?.let { keys.add(it.groupValues[1]) }
            }

            level += trimmed.count { it == '{' } - trimmed.count { it == '}' }
        }

        return keys
    }

    init {
        "the envelope schema declares exactly the fields ApiResponse slumbers" {
            val slumbered = slumberedKeys(typeOf<ApiResponse<String>>(), ApiResponse.ok("hello"))

            withClue("Slumber writes every field, nulls included — nothing may be missing from the schema") {
                slumbered shouldBe setOf("status", "data", "messages", "insights")
            }

            runtimeTs.keysAfter("export function apiResponse", "z.object({") shouldBe slumbered
        }

        "the envelope INTERFACE matches its schema" {
            withClue("the interface is written out by hand rather than inferred, so it can drift alone") {
                runtimeTs.keysAfter("export interface ApiResponse<T>") shouldBe
                        runtimeTs.keysAfter("export function apiResponse", "z.object({")
            }
        }

        "HttpStatusCode slumbers to an object, not a bare number" {
            val slumbered = slumberedKeys(typeOf<HttpStatusCode>(), HttpStatusCode.OK)

            slumbered shouldBe setOf("value", "description")

            runtimeTs.keysAfter("export const HttpStatusCode", "z.object({") shouldBe slumbered
        }

        "Message declares exactly the fields it slumbers" {
            val slumbered = slumberedKeys(typeOf<Message>(), Message.info("hi"))

            slumbered shouldBe setOf("type", "text", "ts")

            runtimeTs.keysAfter("export const Message", "z.object({") shouldBe slumbered
        }

        "Message.type accepts exactly the enum entries that exist" {
            val declared = Regex("type: z\\.enum\\(\\[([^]]*)]\\)").find(runtimeTs)
                ?.groupValues?.get(1)
                ?.split(",")
                ?.map { it.trim().trim('\'') }
                ?.filter { it.isNotEmpty() }
                ?: error("apiResponse.ts does not declare Message.type as a z.enum")

            declared shouldContainExactly Message.Type.entries.map { it.name }
        }

        "Insights declares exactly the fields it slumbers" {
            val slumbered = slumberedKeys(
                typeOf<ApiResponse.Insights>(),
                ApiResponse.Insights(
                    ts = 1L,
                    method = "GET",
                    url = "/x",
                    server = "s",
                    status = HttpStatusCode.OK,
                    durationMs = 1.5,
                ),
            )

            runtimeTs.keysAfter("export const Insights", "z.object({") shouldBe slumbered
        }

        "the locally declared instant shape matches the real MpInstant codec" {
            val slumbered = slumberedKeys(typeOf<MpInstant>(), MpInstant.fromEpochMillis(1_700_000_000_000))

            withClue("apiResponse.ts copies this shape instead of importing datetime.ts") {
                runtimeTs.keysAfter("const instant", "z.object({") shouldBe slumbered
            }

            withClue("the copy must stay in step with the original in datetime.ts") {
                datetimeTs.keysAfter("const timestamped") shouldBe slumbered
            }
        }

        "Message carries the instant shape in its ts field" {
            val slumbered = codec.slumber(
                typeOf<Message>(),
                Message.info("hi", MpInstant.fromEpochMillis(1_700_000_000_000)),
            ) as Map<*, *>

            val ts = slumbered["ts"] as? Map<*, *>
                ?: error("Message.ts did not slumber to a map")

            withClue("if this stops being an object, apiResponse.ts must stop declaring it as one") {
                ts.keys.map { it.toString() }.toSet() shouldBe setOf("ts", "timezone", "human")
            }
        }
    }
}
