package io.peekandpoke.monko

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import org.bson.Document

class HelpersSpec : FreeSpec() {

    init {
        "Document.getNumberOrNull" - {

            "should return number when key exists and value is Int" {
                val doc = Document("count", 42)

                val result = doc.getNumberOrNull("count")

                result shouldBe 42
            }

            "should return number when key exists and value is Long" {
                val doc = Document("count", 1000L)

                val result = doc.getNumberOrNull("count")

                result shouldBe 1000L
            }

            "should return number when key exists and value is Double" {
                val doc = Document("count", 3.14)

                val result = doc.getNumberOrNull("count")

                result shouldBe 3.14
            }

            "should return null when key does not exist" {
                val doc = Document()

                val result = doc.getNumberOrNull("missing")

                result.shouldBeNull()
            }

            "should return null when value is not a Number" {
                val doc = Document("count", "not-a-number")

                val result = doc.getNumberOrNull("count")

                result.shouldBeNull()
            }

            "should return null when value is null" {
                val doc = Document("count", null)

                val result = doc.getNumberOrNull("count")

                result.shouldBeNull()
            }

            "toLong should work on integer result" {
                val doc = Document("count", 42)

                val result = doc.getNumberOrNull("count")?.toLong()

                result shouldBe 42L
            }

            "toLong should work on long result" {
                val doc = Document("count", 9999999999L)

                val result = doc.getNumberOrNull("count")?.toLong()

                result shouldBe 9999999999L
            }
        }
    }
}
