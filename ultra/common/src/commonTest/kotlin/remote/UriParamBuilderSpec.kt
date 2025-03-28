package de.peekandpoke.ultra.common.remote

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe

class UriParamBuilderSpec : StringSpec() {

    init {

        "should create empty param map" {
            val result = UriParamBuilder.uriParams {}

            result shouldBe emptyMap()
        }

        "should set string values correctly" {
            val result = UriParamBuilder.uriParams {
                set("name", "John Doe")
                set("email", "john@example.com")
                set("empty", "")
                set("null", null as String?)
            }

            result["name"] shouldBe UriParamBuilder.StrValue("John Doe")
            result["name"]?.encoded shouldBe "John%20Doe"
            result["email"]?.encoded shouldBe "john%40example.com"
            result["empty"]?.encoded shouldBe ""
            result["null"]?.encoded shouldBe null
        }

        "should set number values correctly" {
            val result = UriParamBuilder.uriParams {
                set("int", 42)
                set("double", 3.14)
                set("zero", 0)
                set("null", null as Number?)
            }

            result["int"] shouldBe UriParamBuilder.NumberValue(42)
            result["int"]?.encoded shouldBe "42"
            result["double"]?.encoded shouldBe "3.14"
            result["zero"]?.encoded shouldBe "0"
            result["null"]?.encoded shouldBe null
        }

        "should set boolean values correctly" {
            val result = UriParamBuilder.uriParams {
                set("true", true)
                set("false", false)
                set("null", null as Boolean?)
            }

            result["true"] shouldBe UriParamBuilder.BooleanValue(true)
            result["true"]?.encoded shouldBe "true"
            result["false"]?.encoded shouldBe "false"
            result["null"]?.encoded shouldBe null
        }

        "should set raw values correctly" {
            val result = UriParamBuilder.uriParams {
                setRaw("raw", "raw value with spaces")
                setRaw("encoded", "already%20encoded")
                setRaw("null", null)
            }

            result["raw"] shouldBe UriParamBuilder.RawValue("raw value with spaces")
            result["raw"]?.encoded shouldBe "raw value with spaces"
            result["encoded"]?.encoded shouldBe "already%20encoded"
            result["null"]?.encoded shouldBe null
        }

        "should override previously set values" {
            val result = UriParamBuilder.uriParams {
                set("param", "first")
                set("param", "second")
            }

            result["param"] shouldBe UriParamBuilder.StrValue("second")
            result["param"]?.encoded shouldBe "second"
        }

        "should create from map" {
            val map = mapOf(
                "name" to "John Doe",
                "age" to "42",
                "empty" to "",
                "null" to null
            )

            val result = UriParamBuilder.of(map)

            result["name"] shouldBe UriParamBuilder.StrValue("John Doe")
            result["name"]?.encoded shouldBe "John%20Doe"
            result["age"]?.encoded shouldBe "42"
            result["empty"]?.encoded shouldBe ""
            result["null"]?.encoded shouldBe null
        }

        "should handle special characters in string values" {

            val result = UriParamBuilder.uriParams {
                set("query", "a+b=c&d")
                set("path", "/path/to/resource")
                set("unicode", "こんにちは")
            }

            result["query"]?.encoded shouldBe "a%2Bb%3Dc%26d"
            result["path"]?.encoded shouldBe "%2Fpath%2Fto%2Fresource"
            result["unicode"]?.encoded shouldBe "%E3%81%93%E3%82%93%E3%81%AB%E3%81%A1%E3%81%AF"
        }

        "should handle different number types" {
            val result = UriParamBuilder.uriParams {
                set("int", 42)
                set("long", 9223372036854775807L)
                set("float", 3.14f)
                set("double", 2.71828)
            }

            result["int"]?.encoded shouldBe "42"
            result["long"]?.encoded shouldBe "9223372036854775807"
            result["float"]?.encoded.shouldNotBeNull()
            result["double"]?.encoded shouldBe "2.71828"
        }
    }
}
