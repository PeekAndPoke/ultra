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
import kotlinx.serialization.json.addJsonArray
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class KotlinxJsonArrayCodecSpec : StringSpec() {
    init {
        "appliesTo matches JsonArray only" {
            KotlinXJsonArrayCodec.appliesTo(JsonArray::class) shouldBe true

            KotlinXJsonArrayCodec.appliesTo(JsonElement::class) shouldBe false
            KotlinXJsonArrayCodec.appliesTo(JsonObject::class) shouldBe false
            KotlinXJsonArrayCodec.appliesTo(JsonPrimitive::class) shouldBe false
            KotlinXJsonArrayCodec.appliesTo(JsonNull::class) shouldBe false
            KotlinXJsonArrayCodec.appliesTo(List::class) shouldBe false
            KotlinXJsonArrayCodec.appliesTo(Any::class) shouldBe false
        }

        "awake converts a List into a JsonArray" {
            val list = listOf("a", 1, true, null)

            val awakened = Codec.default.awake<JsonArray>(list)

            awakened shouldBe buildJsonArray {
                add("a")
                add(1)
                add(true)
                add(JsonNull)
            }
        }

        "awake passes through an existing JsonArray" {
            val original = buildJsonArray {
                add("x")
                add(99)
            }

            val awakened = Codec.default.awake<JsonArray>(original)

            awakened shouldBe original
        }

        "awake of non-List non-JsonArray input fails" {
            shouldThrow<AwakerException> { Codec.default.awake<JsonArray>("hello") }
            shouldThrow<AwakerException> { Codec.default.awake<JsonArray>(42) }
            shouldThrow<AwakerException> { Codec.default.awake<JsonArray>(mapOf("k" to "v")) }
            shouldThrow<AwakerException> { Codec.default.awake<JsonArray>(JsonPrimitive("x")) }
            shouldThrow<AwakerException> { Codec.default.awake<JsonArray>(buildJsonObject { put("k", "v") }) }
        }

        "awake handles nested Lists and Maps" {
            val list = listOf(
                mapOf("k" to "v"),
                listOf(1, 2, 3),
            )

            val awakened = Codec.default.awake<JsonArray>(list)

            awakened shouldBe buildJsonArray {
                addJsonObject { put("k", "v") }
                addJsonArray {
                    add(1)
                    add(2)
                    add(3)
                }
            }
        }

        "slumber of a JsonArray returns a List" {
            val json = buildJsonArray {
                add("a")
                add(1)
                add(true)
                add(JsonNull)
            }

            val slumbered = Codec.default.slumber(json)

            slumbered shouldBe listOf("a", 1L, true, null)
        }

        "roundtrip JsonArray -> List -> JsonArray preserves primitive types" {
            val original = buildJsonArray {
                add(1234567890123L)
                add(3.14)
                add(true)
                add("text")
                add(JsonNull)
            }

            val slumbered = Codec.default.slumber(original)
            val awakened = Codec.default.awake<JsonArray>(slumbered)

            awakened shouldBe original
        }

        "data class with JsonArray field roundtrips" {
            data class Holder(val items: JsonArray)

            val original = Holder(
                items = buildJsonArray {
                    add("a")
                    add(2)
                    addJsonObject { put("k", "v") }
                }
            )

            val slumbered = Codec.default.slumber(original)
            val awakened = Codec.default.awake<Holder>(slumbered)

            awakened shouldBe original
        }

        "nullable JsonArray field can awake from null" {
            data class Holder(val items: JsonArray? = null)

            val awakened = Codec.default.awake<Holder>(mapOf("items" to null))

            awakened shouldBe Holder(items = null)
        }
    }
}
