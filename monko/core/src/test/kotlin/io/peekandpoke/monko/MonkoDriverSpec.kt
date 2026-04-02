package io.peekandpoke.monko

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import org.bson.types.ObjectId

class MonkoDriverSpec : FreeSpec() {

    init {
        "toBsonId" - {

            "should convert a valid ObjectId hex string to ObjectId" {
                val hex = ObjectId().toHexString()
                val result = MonkoDriver.toBsonId(hex)

                result.shouldBeInstanceOf<ObjectId>()
                result.toHexString() shouldBe hex
            }

            "should return a plain string when not a valid ObjectId" {
                val key = "my-custom-key"
                val result = MonkoDriver.toBsonId(key)

                result.shouldBeInstanceOf<String>()
                result shouldBe key
            }

            "should return a plain string for an empty string" {
                val result = MonkoDriver.toBsonId("")

                result.shouldBeInstanceOf<String>()
                result shouldBe ""
            }

            "should return a plain string for a short hex string" {
                val result = MonkoDriver.toBsonId("abcdef")

                result.shouldBeInstanceOf<String>()
                result shouldBe "abcdef"
            }

            "should convert a 24-char hex string to ObjectId" {
                val hex = "deadbeef0123456789abcdef"
                val result = MonkoDriver.toBsonId(hex)

                result.shouldBeInstanceOf<ObjectId>()
                result.toHexString() shouldBe hex
            }

            "should return a plain string for a UUID-style key" {
                val uuid = "550e8400-e29b-41d4-a716-446655440000"
                val result = MonkoDriver.toBsonId(uuid)

                result.shouldBeInstanceOf<String>()
                result shouldBe uuid
            }
        }

        "FindQueryBuilder.page" - {

            "page 1 should skip 0" {
                val builder = MonkoDriver.FindQueryBuilder()
                builder.page(page = 1, epp = 10)

                builder.print().let {
                    it.contains("\"skip\": 0") shouldBe true
                    it.contains("\"limit\": 10") shouldBe true
                }
            }

            "page 2 with epp 10 should skip 10" {
                val builder = MonkoDriver.FindQueryBuilder()
                builder.page(page = 2, epp = 10)

                builder.print().let {
                    it.contains("\"skip\": 10") shouldBe true
                    it.contains("\"limit\": 10") shouldBe true
                }
            }

            "page 3 with epp 5 should skip 10" {
                val builder = MonkoDriver.FindQueryBuilder()
                builder.page(page = 3, epp = 5)

                builder.print().let {
                    it.contains("\"skip\": 10") shouldBe true
                    it.contains("\"limit\": 5") shouldBe true
                }
            }

            "page 0 should be clamped to skip 0" {
                val builder = MonkoDriver.FindQueryBuilder()
                builder.page(page = 0, epp = 10)

                builder.print().let {
                    it.contains("\"skip\": 0") shouldBe true
                    it.contains("\"limit\": 10") shouldBe true
                }
            }

            "negative page should be clamped to skip 0" {
                val builder = MonkoDriver.FindQueryBuilder()
                builder.page(page = -5, epp = 10)

                builder.print().let {
                    it.contains("\"skip\": 0") shouldBe true
                    it.contains("\"limit\": 10") shouldBe true
                }
            }
        }
    }
}
