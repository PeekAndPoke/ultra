package de.peekandpoke.ultra.slumber

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray

class JsonUtilSpec : StringSpec() {

    init {
        "JsonObject.unwrap() should convert simple key-value pairs" {
            val json = buildJsonObject {
                put("string", "value")
                put("number", 42)
                put("boolean", true)
                put("null", JsonNull)
            }

            val result = JsonUtil.run { json.unwrap() }

            result shouldBe mapOf(
                "string" to "value",
                "number" to 42L,
                "boolean" to true,
                "null" to null
            )
        }

        "JsonObject.unwrap() should handle nested objects" {
            val json = buildJsonObject {
                put("nested", buildJsonObject {
                    put("key", "value")
                })
            }

            val result = JsonUtil.run { json.unwrap() }

            result shouldBe mapOf(
                "nested" to mapOf("key" to "value")
            )
        }

        "JsonObject.unwrap() should handle arrays" {
            val json = buildJsonObject {
                putJsonArray("array") {
                    add("string")
                    add(42)
                    add(true)
                }
            }

            val result = JsonUtil.run { json.unwrap() }

            result shouldBe mapOf(
                "array" to listOf("string", 42L, true)
            )
        }

        "Map.toJsonObject() should convert simple key-value pairs" {
            val map = mapOf(
                "string" to "value",
                "number" to 42,
                "boolean" to true,
                "null" to null
            )

            val result = JsonUtil.run { map.toJsonObject() }

            result shouldBe buildJsonObject {
                put("string", "value")
                put("number", 42)
                put("boolean", true)
                put("null", JsonNull)
            }
        }

        "Map.toJsonObject() should handle nested maps" {
            val map = mapOf(
                "nested" to mapOf("key" to "value")
            )

            val result = JsonUtil.run { map.toJsonObject() }

            result shouldBe buildJsonObject {
                put("nested", buildJsonObject {
                    put("key", "value")
                })
            }
        }

        "Map.toJsonObject() should handle lists" {
            val map = mapOf(
                "array" to listOf("string", 42, true)
            )

            val result = JsonUtil.run { map.toJsonObject() }

            result shouldBe buildJsonObject {
                putJsonArray("array") {
                    add("string")
                    add(42)
                    add(true)
                }
            }
        }

        "toJsonElement() should handle all basic types" {
            JsonUtil.run {
                "string".toJsonElement() shouldBe JsonPrimitive("string")
                42.toJsonElement() shouldBe JsonPrimitive(42)
                true.toJsonElement() shouldBe JsonPrimitive(true)
                null.toJsonElement() shouldBe JsonNull
            }
        }

        "toJsonElement() should handle complex objects" {
            val complexObject = mapOf(
                "list" to listOf(1, "two", true),
                "map" to mapOf("key" to "value")
            )

            val result = JsonUtil.run { complexObject.toJsonElement() }

            result shouldBe buildJsonObject {
                putJsonArray("list") {
                    add(1)
                    add("two")
                    add(true)
                }
                put("map", buildJsonObject {
                    put("key", "value")
                })
            }
        }
    }
}
