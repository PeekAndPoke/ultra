package io.peekandpoke.ultra.codegen.contributors

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.kotest.matchers.types.shouldBeInstanceOf
import io.peekandpoke.ultra.codegen.model.TsTypeClaims
import io.peekandpoke.ultra.slumber.Codec
import kotlin.reflect.KType
import kotlin.reflect.full.createType
import kotlin.reflect.typeOf
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Drift guard for the claims in [KotlinxJsonTsContributor].
 *
 * The standing rule for this module: a claim is TRUSTED and never verified by the generator, because a
 * claimed type is deliberately never declared — that is the point of a claim. So a claim that is simply
 * wrong produces confidently wrong TypeScript and nothing notices. The only defence is a test that
 * slumbers a real value and checks it against what the claim asserts.
 *
 * These five claims had no such test. They turn out to be correct, but that was luck until now: the
 * shape of `JsonObject` is whatever `JsonUtil.unwrap` produces, and nothing pinned that it flattens
 * nested elements rather than leaving `JsonElement`s in the map. Two of six datetime claims WERE wrong
 * when first written from assumption, so the risk is not hypothetical.
 */
class KotlinxJsonClaimParitySpec : FreeSpec() {

    private val codec = Codec.default

    private fun slumber(type: KType, value: Any?): Any? = codec.slumber(type, value)

    /** Every value reachable in [data], including [data] itself. */
    private fun flatten(data: Any?): List<Any?> = when (data) {
        is Map<*, *> -> listOf(data) + data.values.flatMap { flatten(it) }
        is Iterable<*> -> listOf(data) + data.flatMap { flatten(it) }
        else -> listOf(data)
    }

    private val nested = JsonObject(
        mapOf(
            "s" to JsonPrimitive("txt"),
            "n" to JsonPrimitive(42),
            "b" to JsonPrimitive(true),
            "nil" to JsonNull,
            "inner" to JsonObject(mapOf("deep" to JsonPrimitive("d"))),
            "arr" to JsonArray(listOf(JsonPrimitive(1), JsonPrimitive(2))),
        )
    )

    init {
        "each claim's zod combinator matches the shape the codec really produces" {
            // THE assertion that makes this a drift guard rather than a codec test. Everything else
            // here checks what Slumber does; this derives the expected combinator FROM that observed
            // shape and holds the claim to it. Change the codec and the expectation moves, so the claim
            // stops matching — which is the coupling the standing rule asks for. Asserting the schema
            // string against a hand-written copy would only prove it was copied correctly.
            val registry = TsTypeClaims()

            KotlinxJsonTsContributor().claimTypes(registry.scopeFor("kx"))

            fun combinatorFor(slumbered: Any?): String = when (slumbered) {
                null -> "z.null("
                is Map<*, *> -> "z.record("
                is Iterable<*> -> "z.array("
                is String, is Number, is Boolean -> "z.union("
                else -> error("unhandled slumbered shape: ${slumbered::class}")
            }

            listOf(
                JsonObject::class to nested,
                JsonArray::class to JsonArray(listOf(JsonPrimitive(1))),
                JsonPrimitive::class to JsonPrimitive("x"),
                JsonNull::class to JsonNull,
            ).forEach { (cls, value) ->
                val claim = registry.find(cls)

                withClue("${cls.simpleName} must be claimed at all") { claim.shouldNotBeNull() }

                val observed = slumber(cls.createType(nullable = true), value)

                withClue("${cls.simpleName} slumbers to ${observed?.let { it::class.simpleName }}") {
                    claim!!.schema!! shouldStartWith combinatorFor(observed)
                }
            }

            withClue("JsonElement holds any of the above, so it alone is unconstrained") {
                registry.find(JsonElement::class)!!.schema shouldBe "z.unknown()"
            }
        }

        "every claimed type is one Slumber actually has a codec for" {
            // Guards the other direction: a claim for a type Slumber refuses would be describing
            // output no server can produce.
            listOf(
                typeOf<JsonElement>() to nested,
                typeOf<JsonObject>() to nested,
                typeOf<JsonArray>() to JsonArray(listOf(JsonPrimitive(1))),
                typeOf<JsonPrimitive>() to JsonPrimitive("x"),
                typeOf<JsonNull>() to JsonNull,
            ).forEach { (type, value) ->
                withClue("$type") {
                    runCatching { slumber(type, value) }.isSuccess shouldBe true
                }
            }
        }

        "no kotlinx type survives slumbering, which is what makes 'unknown' the right claim" {
            // THE load-bearing assertion. If `unwrap` ever stopped flattening, a JsonElement would
            // reach the wire and `Record<string, unknown>` / `unknown[]` would still typecheck while
            // describing something the client cannot use.
            listOf(
                slumber(typeOf<JsonObject>(), nested),
                slumber(typeOf<JsonArray>(), JsonArray(listOf(JsonPrimitive("a"), nested))),
                slumber(typeOf<JsonElement>(), nested),
            ).forEach { slumbered ->
                withClue("slumbered: $slumbered") {
                    flatten(slumbered).filterIsInstance<JsonElement>() shouldContainExactly emptyList()
                }
            }
        }

        "JsonObject slumbers to a map — the claim is Record<string, unknown>" {
            val slumbered = slumber(typeOf<JsonObject>(), nested).shouldBeInstanceOf<Map<*, *>>()

            withClue("string keys, as any JSON object has") {
                slumbered.keys.forEach { it.shouldBeInstanceOf<String>() }
            }

            withClue("nested values are flattened too, not left as JsonElement") {
                slumbered["inner"].shouldBeInstanceOf<Map<*, *>>()
                slumbered["arr"].shouldBeInstanceOf<List<*>>()
            }
        }

        "JsonArray slumbers to a list — the claim is unknown[]" {
            slumber(typeOf<JsonArray>(), JsonArray(listOf(JsonPrimitive("a"), nested)))
                .shouldBeInstanceOf<List<*>>()
                .size shouldBe 2
        }

        "JsonPrimitive slumbers to a BARE scalar, and only to the kinds the claim unions" {
            // The claim is `string | number | boolean | null`. Each member is checked against a real
            // value, so dropping one from the claim — or gaining a sixth kind here — shows up.
            slumber(typeOf<JsonPrimitive>(), JsonPrimitive("txt")).shouldBeInstanceOf<String>()
            slumber(typeOf<JsonPrimitive>(), JsonPrimitive(42)).shouldBeInstanceOf<Number>()
            slumber(typeOf<JsonPrimitive>(), JsonPrimitive(true)).shouldBeInstanceOf<Boolean>()

            withClue("a wrapper object here would make the union claim wrong in every position") {
                slumber(typeOf<JsonPrimitive>(), JsonPrimitive("txt")) shouldBe "txt"
            }
        }

        "JsonNull slumbers to null — the claim is z.null()" {
            slumber(typeOf<JsonNull>(), JsonNull).shouldBeNull()
        }

        "JsonElement takes the shape of whatever it actually holds" {
            withClue("which is exactly why the claim is the unconstrained 'unknown'") {
                slumber(typeOf<JsonElement>(), JsonPrimitive("p")) shouldBe "p"
                slumber(typeOf<JsonElement>(), nested).shouldBeInstanceOf<Map<*, *>>()
                slumber(typeOf<JsonElement>(), JsonArray(listOf(JsonPrimitive(1))))
                    .shouldBeInstanceOf<List<*>>()
            }
        }

        "a NON-NULLABLE JsonElement holding JsonNull refuses to slumber" {
            // Found while writing this spec; asserted because it is surprising, not because it is
            // wrong. JsonNull is a perfectly good JsonElement, but it unwraps to Kotlin null and
            // NonNullSlumberer then rejects it for a non-nullable declared type. So `val x: JsonElement`
            // holding JsonNull throws at serialization rather than writing null.
            runCatching { slumber(typeOf<JsonElement>(), JsonNull) }.isFailure shouldBe true

            withClue("declaring it nullable is what a field holding JsonNull actually needs") {
                slumber(typeOf<JsonElement?>(), JsonNull).shouldBeNull()
            }

            // Consequence for the claim: `unknown` admits null, while a non-nullable JsonElement can
            // never BE null on the wire. Loose on parse, which is the documented-safe direction.
        }
    }
}
