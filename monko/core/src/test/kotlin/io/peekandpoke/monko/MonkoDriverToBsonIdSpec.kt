package io.peekandpoke.monko

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.bson.types.ObjectId

class MonkoDriverToBsonIdSpec : FreeSpec() {

    init {
        "toBsonId" - {

            "should convert a valid 24-char hex string to ObjectId" {
                val hex = ObjectId().toHexString()
                val result = MonkoDriver.toBsonId(hex)

                result.shouldBeInstanceOf<ObjectId>()
                result.toHexString() shouldBe hex
            }

            "should return String for non-hex key" {
                val key = "my-custom-key"
                val result = MonkoDriver.toBsonId(key)

                result.shouldBeInstanceOf<String>()
                result shouldBe key
            }

            "should return String for empty string" {
                val result = MonkoDriver.toBsonId("")

                result.shouldBeInstanceOf<String>()
                result shouldBe ""
            }

            "should return String for short hex" {
                val result = MonkoDriver.toBsonId("abcdef")

                result.shouldBeInstanceOf<String>()
                result shouldBe "abcdef"
            }

            "should convert deadbeef pattern to ObjectId" {
                val hex = "deadbeef0123456789abcdef"
                val result = MonkoDriver.toBsonId(hex)

                result.shouldBeInstanceOf<ObjectId>()
                result.toHexString() shouldBe hex
            }

            "should return String for UUID-style key" {
                val uuid = "550e8400-e29b-41d4-a716-446655440000"
                val result = MonkoDriver.toBsonId(uuid)

                result.shouldBeInstanceOf<String>()
                result shouldBe uuid
            }

            "should return String for numeric key" {
                val result = MonkoDriver.toBsonId("12345")

                result.shouldBeInstanceOf<String>()
                result shouldBe "12345"
            }

            "should return String for key with special characters" {
                val key = "user@example.com"
                val result = MonkoDriver.toBsonId(key)

                result.shouldBeInstanceOf<String>()
                result shouldBe key
            }

            "should handle 24-char non-hex string as String" {
                // 24 chars but contains non-hex chars
                val key = "gggggggggggggggggggggggg"
                val result = MonkoDriver.toBsonId(key)

                result.shouldBeInstanceOf<String>()
                result shouldBe key
            }

            "should handle all-zero ObjectId" {
                val hex = "000000000000000000000000"
                val result = MonkoDriver.toBsonId(hex)

                result.shouldBeInstanceOf<ObjectId>()
                result.toHexString() shouldBe hex
            }

            "should handle all-f ObjectId" {
                val hex = "ffffffffffffffffffffffff"
                val result = MonkoDriver.toBsonId(hex)

                result.shouldBeInstanceOf<ObjectId>()
                result.toHexString() shouldBe hex
            }
        }
    }
}
