package de.peekandpoke.ultra.common

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe

class PlaceholdersSpec : StringSpec() {
    // Define enum classes at class level
    enum class TestEnum { ONE, TWO, THREE }
    enum class SmallEnum { FOO, BAR }

    init {
        "TripleHash placeholders should work with enums" {
            val placeholders = Placeholders.TripleHash<TestEnum>()

            placeholders.patterns shouldContainExactlyInAnyOrder setOf(
                "###ONE###",
                "###TWO###",
                "###THREE###"
            )
        }

        "TripleHash placeholders should work with custom toString" {
            val placeholders = Placeholders.TripleHash<TestEnum> { it.name.lowercase() }

            placeholders.patterns shouldContainExactlyInAnyOrder setOf(
                "###one###",
                "###two###",
                "###three###"
            )
        }

        "DoubleCurly placeholders should work with enums" {
            val placeholders = Placeholders.DoubleCurly<SmallEnum>()

            placeholders.patterns shouldContainExactlyInAnyOrder setOf(
                "{{FOO}}",
                "{{BAR}}"
            )
        }

        "DoubleCurly placeholders should work with custom toString" {
            val placeholders = Placeholders.DoubleCurly<SmallEnum> { it.name.lowercase() }

            placeholders.patterns shouldContainExactlyInAnyOrder setOf(
                "{{foo}}",
                "{{bar}}"
            )
        }

        "findErrorsIn should detect invalid patterns" {
            val tripleHash = Placeholders.TripleHash<TestEnum>()
            tripleHash.findErrorsIn("###ONE### ###INVALID### ###TWO###") shouldContainExactlyInAnyOrder setOf(
                "###INVALID###"
            )

            val doubleCurly = Placeholders.DoubleCurly<SmallEnum>()
            doubleCurly.findErrorsIn("{{FOO}} {{INVALID}} {{BAR}}") shouldContainExactlyInAnyOrder setOf(
                "{{INVALID}}"
            )
        }

        "validate should work correctly" {
            val placeholders = Placeholders.TripleHash<TestEnum>()

            placeholders.validate("###ONE### ###TWO###") shouldBe true
            placeholders.validate("###ONE### ###INVALID###") shouldBe false
        }

        "fill should replace placeholders with values" {
            val tripleHash = Placeholders.TripleHash<TestEnum>()
            val filled = tripleHash.fill { it.name.lowercase() }

            filled("Test ###ONE### and ###TWO###") shouldBe "Test one and two"

            val doubleCurly = Placeholders.DoubleCurly<SmallEnum>()
            val filledCurly = doubleCurly.fill { it.name.lowercase() }

            filledCurly("Test {{FOO}} and {{BAR}}") shouldBe "Test foo and bar"
        }

        "namesToPatterns should contain correct mappings" {
            val placeholders = Placeholders.DoubleCurly<SmallEnum>()

            placeholders.namesToPatterns shouldContainExactlyInAnyOrder setOf(
                Placeholders.NameToPattern("FOO", "{{FOO}}"),
                Placeholders.NameToPattern("BAR", "{{BAR}}")
            )
        }

        "should work with custom types" {
            data class CustomType(val value: String)

            val values = setOf(CustomType("one"), CustomType("two"))

            val tripleHash = Placeholders.TripleHash(values) { it.value }
            tripleHash.patterns shouldContainExactlyInAnyOrder setOf(
                "###one###",
                "###two###"
            )

            val doubleCurly = Placeholders.DoubleCurly(values) { it.value }
            doubleCurly.patterns shouldContainExactlyInAnyOrder setOf(
                "{{one}}",
                "{{two}}"
            )
        }

        "fill() should handle empty text" {
            val placeholders = Placeholders.TripleHash<TestEnum>()
            val filled = placeholders.fill { it.name }

            filled("") shouldBe ""
        }

        "fill() should handle text without placeholders" {
            val placeholders = Placeholders.TripleHash<TestEnum>()
            val filled = placeholders.fill { it.name }

            filled("Simple text") shouldBe "Simple text"
        }

        "fill() should replace multiple occurrences of the same placeholder" {
            val placeholders = Placeholders.TripleHash<TestEnum>()
            val filled = placeholders.fill { it.name.lowercase() }

            filled("###ONE### and ###ONE### and ###ONE###") shouldBe "one and one and one"
        }

        "fill() should handle mixed placeholders" {
            val placeholders = Placeholders.TripleHash<TestEnum>()
            val filled = placeholders.fill { it.name.lowercase() }

            filled("Start ###ONE### middle ###TWO### ###THREE### end") shouldBe
                    "Start one middle two three end"
        }

        "fill() should work with complex transformations" {
            val placeholders = Placeholders.TripleHash<TestEnum>()
            val filled = placeholders.fill {
                when (it) {
                    TestEnum.ONE -> "1st"
                    TestEnum.TWO -> "2nd"
                    TestEnum.THREE -> "3rd"
                }
            }

            filled("###ONE###, ###TWO###, ###THREE###") shouldBe "1st, 2nd, 3rd"
        }

        "DoubleCurly fill() should handle special characters in replacement" {
            val placeholders = Placeholders.DoubleCurly<SmallEnum>()
            val filled = placeholders.fill {
                when (it) {
                    SmallEnum.FOO -> "Hello, World!"
                    SmallEnum.BAR -> "Special $&*@# chars"
                }
            }

            filled("{{FOO}} and {{BAR}}") shouldBe "Hello, World! and Special $&*@# chars"
        }

        "fill() should work with both invoke() and replace() methods" {
            val placeholders = Placeholders.TripleHash<TestEnum>()
            val filled = placeholders.fill { it.name.lowercase() }

            // Using invoke operator
            filled("###ONE###") shouldBe "one"

            // Using replace method
            filled.replace("###ONE###") shouldBe "one"

            // Both should give same results
            val text = "###ONE### ###TWO###"
            filled(text) shouldBe filled.replace(text)
        }

        "fill() should preserve surrounding whitespace and punctuation" {
            val placeholders = Placeholders.DoubleCurly<SmallEnum>()
            val filled = placeholders.fill { it.name.lowercase() }

            filled("  {{FOO}}, {{BAR}}!  ") shouldBe "  foo, bar!  "
        }
    }
}
