package io.peekandpoke.ultra.slumber.builtin.kotlinx

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.slumber.AwakerException
import io.peekandpoke.ultra.slumber.Codec
import io.peekandpoke.ultra.slumber.awake
import io.peekandpoke.ultra.slumber.slumber
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

class KotlinxJsonObjectCodecSpec : StringSpec() {
    init {
        "appliesTo matches JsonObject only" {
            KotlinXJsonObjectCodec.appliesTo(JsonObject::class) shouldBe true

            KotlinXJsonObjectCodec.appliesTo(JsonElement::class) shouldBe false
            KotlinXJsonObjectCodec.appliesTo(JsonArray::class) shouldBe false
            KotlinXJsonObjectCodec.appliesTo(JsonPrimitive::class) shouldBe false
            KotlinXJsonObjectCodec.appliesTo(JsonNull::class) shouldBe false
            KotlinXJsonObjectCodec.appliesTo(Map::class) shouldBe false
            KotlinXJsonObjectCodec.appliesTo(Any::class) shouldBe false
        }

        "awake converts a Map into a JsonObject" {
            val map = mapOf(
                "string" to "value",
                "number" to 42,
                "boolean" to true,
                "null" to null,
            )

            val awakened = Codec.default.awake<JsonObject>(map)

            awakened shouldBe buildJsonObject {
                put("string", "value")
                put("number", 42)
                put("boolean", true)
                put("null", JsonNull)
            }
        }

        "awake passes through an existing JsonObject" {
            val original = buildJsonObject {
                put("k", "v")
                put("n", 7)
            }

            val awakened = Codec.default.awake<JsonObject>(original)

            awakened shouldBe original
        }

        "awake of a non-Map non-JsonObject input fails (rejects wrong subtypes)" {
            // Old behavior would silently produce a JsonPrimitive from a String input.
            // Now: dedicated codec returns null and the non-null wrapper throws.
            shouldThrow<AwakerException> { Codec.default.awake<JsonObject>("hello") }
            shouldThrow<AwakerException> { Codec.default.awake<JsonObject>(42) }
            shouldThrow<AwakerException> { Codec.default.awake<JsonObject>(true) }
            shouldThrow<AwakerException> { Codec.default.awake<JsonObject>(listOf(1, 2, 3)) }
            shouldThrow<AwakerException> { Codec.default.awake<JsonObject>(JsonPrimitive("x")) }
            shouldThrow<AwakerException> { Codec.default.awake<JsonObject>(buildJsonArray { add(1) }) }
        }

        "awake handles nested Maps and Lists" {
            val map = mapOf(
                "nested" to mapOf("k" to "v"),
                "list" to listOf("a", 1, true),
            )

            val awakened = Codec.default.awake<JsonObject>(map)

            awakened shouldBe buildJsonObject {
                putJsonObject("nested") { put("k", "v") }
                putJsonArray("list") {
                    add("a")
                    add(1)
                    add(true)
                }
            }
        }

        "slumber of a JsonObject returns a Map" {
            val json = buildJsonObject {
                put("string", "value")
                put("number", 42)
                put("boolean", true)
                put("null", JsonNull)
            }

            val slumbered = Codec.default.slumber(json)

            slumbered shouldBe mapOf(
                "string" to "value",
                "number" to 42L,
                "boolean" to true,
                "null" to null,
            )
        }

        "roundtrip JsonObject -> Map -> JsonObject preserves primitive types" {
            // This is the case that was previously broken: re-wrapping a JsonPrimitive(123)
            // through `else -> JsonPrimitive(toString())` would stringify it.
            val original = buildJsonObject {
                put("long", 1234567890123L)
                put("double", 3.14)
                put("bool", true)
                put("text", "abc")
                put("null", JsonNull)
            }

            val slumbered = Codec.default.slumber(original)
            val awakened = Codec.default.awake<JsonObject>(slumbered)

            awakened shouldBe original
        }

        "data class with JsonObject field roundtrips" {
            data class Holder(val payload: JsonObject)

            val original = Holder(
                payload = buildJsonObject {
                    put("ok", true)
                    putJsonArray("items") {
                        add("a")
                        add(2)
                    }
                }
            )

            val slumbered = Codec.default.slumber(original)
            val awakened = Codec.default.awake<Holder>(slumbered)

            awakened shouldBe original
        }

        "nullable JsonObject field can awake from null" {
            data class Holder(val payload: JsonObject? = null)

            val awakened = Codec.default.awake<Holder>(mapOf("payload" to null))

            awakened shouldBe Holder(payload = null)
        }
    }
}
