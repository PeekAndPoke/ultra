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

class KotlinxJsonPrimitiveCodecSpec : StringSpec() {
    init {
        "appliesTo matches JsonPrimitive and its subtypes" {
            // In kotlinx.serialization, JsonNull : JsonPrimitive — so isAssignableFrom is true.
            // BuiltInModule registers KotlinXJsonNullCodec BEFORE KotlinXJsonPrimitiveCodec,
            // which means JsonNull is still routed to its dedicated codec at runtime.
            KotlinXJsonPrimitiveCodec.appliesTo(JsonPrimitive::class) shouldBe true
            KotlinXJsonPrimitiveCodec.appliesTo(JsonNull::class) shouldBe true

            KotlinXJsonPrimitiveCodec.appliesTo(JsonElement::class) shouldBe false
            KotlinXJsonPrimitiveCodec.appliesTo(JsonObject::class) shouldBe false
            KotlinXJsonPrimitiveCodec.appliesTo(JsonArray::class) shouldBe false
            KotlinXJsonPrimitiveCodec.appliesTo(String::class) shouldBe false
            KotlinXJsonPrimitiveCodec.appliesTo(Number::class) shouldBe false
        }

        "awake wraps scalars into JsonPrimitive" {
            Codec.default.awake<JsonPrimitive>("hello") shouldBe JsonPrimitive("hello")
            Codec.default.awake<JsonPrimitive>(42) shouldBe JsonPrimitive(42)
            Codec.default.awake<JsonPrimitive>(1234567890123L) shouldBe JsonPrimitive(1234567890123L)
            Codec.default.awake<JsonPrimitive>(3.14) shouldBe JsonPrimitive(3.14)
            Codec.default.awake<JsonPrimitive>(true) shouldBe JsonPrimitive(true)
            Codec.default.awake<JsonPrimitive>(false) shouldBe JsonPrimitive(false)
        }

        "awake passes through an existing JsonPrimitive without re-wrapping" {
            // This is the regression test for the toJsonElement re-wrap bug:
            // a JsonPrimitive(123) used to be stringified as JsonPrimitive("123").
            val original = JsonPrimitive(123L)

            val awakened = Codec.default.awake<JsonPrimitive>(original)

            awakened shouldBe original
        }

        "awake of non-scalar non-JsonPrimitive input fails" {
            shouldThrow<AwakerException> { Codec.default.awake<JsonPrimitive>(mapOf("k" to "v")) }
            shouldThrow<AwakerException> { Codec.default.awake<JsonPrimitive>(listOf(1, 2)) }
            shouldThrow<AwakerException> { Codec.default.awake<JsonPrimitive>(buildJsonObject { put("k", "v") }) }
            shouldThrow<AwakerException> { Codec.default.awake<JsonPrimitive>(buildJsonArray { add(1) }) }
        }

        "slumber of a JsonPrimitive returns the underlying value" {
            Codec.default.slumber(JsonPrimitive("hello")) shouldBe "hello"
            Codec.default.slumber(JsonPrimitive(true)) shouldBe true
            Codec.default.slumber(JsonPrimitive(42)) shouldBe 42L
            Codec.default.slumber(JsonPrimitive(3.14)) shouldBe 3.14
        }

        "roundtrip JsonPrimitive -> scalar -> JsonPrimitive preserves type" {
            val cases = listOf(
                JsonPrimitive("text"),
                JsonPrimitive(true),
                JsonPrimitive(1234567890123L),
                JsonPrimitive(3.14),
            )

            for (original in cases) {
                val slumbered = Codec.default.slumber(original)
                val awakened = Codec.default.awake<JsonPrimitive>(slumbered)
                awakened shouldBe original
            }
        }

        "data class with JsonPrimitive field roundtrips" {
            data class Holder(val value: JsonPrimitive)

            val original = Holder(value = JsonPrimitive(42L))

            val slumbered = Codec.default.slumber(original)
            val awakened = Codec.default.awake<Holder>(slumbered)

            awakened shouldBe original
        }

        "nullable JsonPrimitive field can awake from null" {
            data class Holder(val value: JsonPrimitive? = null)

            val awakened = Codec.default.awake<Holder>(mapOf("value" to null))

            awakened shouldBe Holder(value = null)
        }
    }
}
