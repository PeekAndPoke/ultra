package de.peekandpoke.ultra.slumber.builtin.kotlinx

import de.peekandpoke.ultra.slumber.Codec
import de.peekandpoke.ultra.slumber.JsonUtil.toJsonElement
import de.peekandpoke.ultra.slumber.JsonUtil.unwrap
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

class KotlinxJsonCodecSpec : StringSpec() {
    init {
        "appliesTo should return true for JsonElement classes" {
            KotlinXJsonCodec.appliesTo(JsonObject::class) shouldBe true
            KotlinXJsonCodec.appliesTo(JsonArray::class) shouldBe true
            KotlinXJsonCodec.appliesTo(JsonPrimitive::class) shouldBe true

            KotlinXJsonCodec.appliesTo(Any::class) shouldBe false
            KotlinXJsonCodec.appliesTo(Number::class) shouldBe false
            KotlinXJsonCodec.appliesTo(Nothing::class) shouldBe false
        }

        "unwrap should handle primitive types correctly" {
            JsonPrimitive("test").unwrap() shouldBe "test"
            JsonPrimitive(true).unwrap() shouldBe true
            JsonPrimitive(42).unwrap() shouldBe 42L
            JsonPrimitive(3.14).unwrap() shouldBe 3.14
            JsonNull.unwrap() shouldBe null
        }

        "unwrap should handle complex objects" {
            val json = buildJsonObject {
                put("string", "value")
                put("number", 42)
                put("boolean", true)
                put("null", JsonNull)
                putJsonArray("array") {
                    add("item1")
                    add(123)
                }
                putJsonObject("nested") {
                    put("key", "value")
                }
            }

            val unwrapped = json.unwrap()

            unwrapped shouldBe mapOf(
                "string" to "value",
                "number" to 42L,
                "boolean" to true,
                "null" to null,
                "array" to listOf("item1", 123L),
                "nested" to mapOf("key" to "value")
            )
        }

        "toJsonElement should convert primitive types" {
            "test".toJsonElement() shouldBe JsonPrimitive("test")
            true.toJsonElement() shouldBe JsonPrimitive(true)
            42.toJsonElement() shouldBe JsonPrimitive(42)
            3.14.toJsonElement() shouldBe JsonPrimitive(3.14)
            null.toJsonElement() shouldBe JsonNull
        }

        "toJsonElement should convert complex structures" {
            val map = mapOf(
                "string" to "value",
                "number" to 42,
                "boolean" to true,
                "null" to null,
                "array" to listOf("item1", 123),
                "nested" to mapOf("key" to "value")
            )

            val expected = buildJsonObject {
                put("string", "value")
                put("number", 42)
                put("boolean", true)
                put("null", JsonNull)
                putJsonArray("array") {
                    add("item1")
                    add(123)
                }
                putJsonObject("nested") {
                    put("key", "value")
                }
            }

            map.toJsonElement() shouldBe expected
        }

        "slumber should convert JsonElement to basic types" {
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
                "null" to null
            )
        }

        "awake should convert Map to JsonObject" {
            val map = mapOf(
                "string" to "value",
                "number" to 42,
                "boolean" to true,
                "null" to null
            )

            val awakened = Codec.default.awake<JsonObject>(map)

            awakened shouldBe buildJsonObject {
                put("string", "value")
                put("number", 42)
                put("boolean", true)
                put("null", JsonNull)
            }
        }

        "slumber and awake roundtrip" {
            val json = buildJsonObject {
                put("string", "value")
                put("number", 42)
                put("boolean", true)
                put("null", JsonNull)
            }

            val slumbered = Codec.default.slumber(json)
            val awakened = Codec.default.awake<JsonObject>(slumbered)

            awakened shouldBe json
        }

        "awake and slumber roundtrip" {
            val map = mapOf(
                "string" to "value",
                "number" to 42,
                "boolean" to true,
                "null" to null
            )

            val awakened = Codec.default.awake<JsonObject>(map)
            val slumbered = Codec.default.slumber(awakened)

            slumbered shouldBe map
        }

        "Slumber integration example" {

            data class DataClass(
                val string: String,
                val json: JsonElement,
            )

            val instance = DataClass(
                string = "test",
                json = buildJsonObject {
                    put("key", "value")
                }
            )

            val slumbered = Codec.default.slumber(instance)

            slumbered shouldBe mapOf(
                "string" to "test",
                "json" to mapOf("key" to "value")
            )
        }

        "Slumber integration example with nullable" {

            data class DataClass(
                val string: String,
                val json: JsonElement? = null,
            )

            val instance = DataClass(
                string = "test",
                json = null,
            )

            val slumbered = Codec.default.slumber(instance)

            slumbered shouldBe mapOf(
                "string" to "test",
                "json" to null,
            )
        }

        "Slumber integration example with JsonNull as child" {

            data class DataClass(
                val string: String,
                val json: JsonElement? = null,
            )

            val instance = DataClass(
                string = "test",
                json = buildJsonObject {
                    put("jsonNull", JsonNull)
                },
            )

            val slumbered = Codec.default.slumber(instance)

            slumbered shouldBe mapOf(
                "string" to "test",
                "json" to mapOf(
                    "jsonNull" to null,
                ),
            )
        }

        "Awake integration example" {

            data class DataClass(
                val string: String,
                val json: JsonElement,
            )

            val data = mapOf(
                "string" to "test",
                "json" to mapOf("key" to "value")
            )

            val awakened = Codec.default.awake<DataClass>(data)

            awakened shouldBe DataClass(
                string = "test",
                json = buildJsonObject {
                    put("key", "value")
                }
            )
        }

        "Awake integration example with nullable" {

            data class DataClass(
                val string: String,
                val json: JsonElement? = null,
            )

            val data = mapOf(
                "string" to "test",
                "json" to null,
            )

            val awakened = Codec.default.awake<DataClass>(data)

            awakened shouldBe DataClass(
                string = "test",
                json = JsonNull,
            )
        }

        "Slumber JsonNull" {
            val slumbered = Codec.default.slumber(JsonNull)

            slumbered shouldBe null
        }

        "Awake null to JsonNull" {
            val awoken = Codec.default.awake<JsonNull>(null)

            awoken shouldBe JsonNull
        }
    }
}
