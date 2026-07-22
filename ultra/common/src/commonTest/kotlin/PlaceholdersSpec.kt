package io.peekandpoke.ultra.common

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

        // --- Adversarial / security regressions: substitution must be single-pass -----------------

        "SECURITY: a substituted value containing a placeholder is NOT re-interpreted" {
            // FOO's value is literally the BAR placeholder. It must be emitted verbatim, never
            // expanded into BAR's (potentially secret) value. This is the second-order injection.
            val placeholders = Placeholders.DoubleCurly<SmallEnum>()
            val filled = placeholders.fill {
                when (it) {
                    SmallEnum.FOO -> "{{BAR}}"
                    SmallEnum.BAR -> "SECRET"
                }
            }

            filled("name={{FOO}} token={{BAR}}") shouldBe "name={{BAR}} token=SECRET"
        }

        "SECURITY: chained values are not amplified (no billion-laughs)" {
            // Each value expands to two of the next placeholder. Single-pass => the nested
            // placeholders are emitted literally, not recursively expanded.
            val placeholders = Placeholders.DoubleCurly<TestEnum>()
            val filled = placeholders.fill {
                when (it) {
                    TestEnum.ONE -> "{{TWO}}{{TWO}}"
                    TestEnum.TWO -> "{{THREE}}{{THREE}}"
                    TestEnum.THREE -> "x"
                }
            }

            filled("{{ONE}}") shouldBe "{{TWO}}{{TWO}}"
        }

        "a value equal to its own placeholder is emitted literally, not looped" {
            val placeholders = Placeholders.DoubleCurly<SmallEnum>()
            val filled = placeholders.fill {
                when (it) {
                    SmallEnum.FOO -> "{{FOO}}"
                    SmallEnum.BAR -> "bar"
                }
            }

            filled("{{FOO}} {{BAR}}") shouldBe "{{FOO}} bar"
        }

        "overlapping placeholder names resolve to the intended (longest) pattern" {
            data class V(val n: String)

            val placeholders = Placeholders.DoubleCurly(setOf(V("a"), V("ab"))) { it.n }
            val filled = placeholders.fill { it.n.uppercase() }

            filled("{{a}}-{{ab}}") shouldBe "A-AB"
        }

        "an empty placeholder set leaves the text untouched" {
            data class V(val n: String)

            val placeholders = Placeholders.DoubleCurly(emptySet<V>()) { it.n }
            val filled = placeholders.fill { it.n }

            filled("plain {{x}} text") shouldBe "plain {{x}} text"
        }

        "an unknown placeholder in the text is left untouched" {
            val placeholders = Placeholders.DoubleCurly<SmallEnum>()
            val filled = placeholders.fill { it.name.lowercase() }

            filled("{{FOO}} and {{UNKNOWN}}") shouldBe "foo and {{UNKNOWN}}"
        }

        "adjacent placeholders are each substituted exactly once" {
            val placeholders = Placeholders.DoubleCurly<SmallEnum>()
            val filled = placeholders.fill { it.name.lowercase() }

            filled("{{FOO}}{{BAR}}{{FOO}}") shouldBe "foobarfoo"
        }

        "a value mapping to empty string removes the placeholder" {
            val placeholders = Placeholders.DoubleCurly<SmallEnum>()
            val filled = placeholders.fill { if (it == SmallEnum.FOO) "" else "bar" }

            filled("[{{FOO}}][{{BAR}}]") shouldBe "[][bar]"
        }

        "SECURITY: TripleHash substitution is also single-pass" {
            val placeholders = Placeholders.TripleHash<SmallEnum>()
            val filled = placeholders.fill {
                when (it) {
                    SmallEnum.FOO -> "###BAR###"
                    SmallEnum.BAR -> "SECRET"
                }
            }

            filled("name=###FOO### token=###BAR###") shouldBe "name=###BAR### token=SECRET"
        }

        "TripleHash overlapping names resolve to the intended (longest) pattern" {
            data class V(val n: String)

            val placeholders = Placeholders.TripleHash(setOf(V("a"), V("ab"))) { it.n }
            val filled = placeholders.fill { it.n.uppercase() }

            filled("###a###-###ab###") shouldBe "A-AB"
        }

        "a placeholder NAME containing a regex metachar is matched literally" {
            data class V(val n: String)

            // '|' is the alternation separator; it must be escaped, not treated as regex syntax.
            val placeholders = Placeholders.DoubleCurly(setOf(V("a|b"), V("a"))) { it.n }
            val filled = placeholders.fill { it.n.uppercase() }

            filled("{{a|b}} {{a}}") shouldBe "A|B A"
        }

        "SECURITY: a value concatenating with following text to form a pattern is not expanded" {
            // template `{{FOO}}AR}}` + FOO->`{{B` would concatenate to `{{BAR}}`; single-pass must
            // NOT re-scan it. (Old multi-pass code produced "SECRET" here.)
            val placeholders = Placeholders.DoubleCurly<SmallEnum>()
            val filled = placeholders.fill {
                when (it) {
                    SmallEnum.FOO -> "{{B"
                    SmallEnum.BAR -> "SECRET"
                }
            }

            filled("{{FOO}}AR}}") shouldBe "{{BAR}}"
        }
    }
}
