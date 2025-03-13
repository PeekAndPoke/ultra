package de.peekandpoke.ultra.mutator.unit

import de.peekandpoke.ultra.mutator.isSameAs
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.withClue
import io.kotest.core.spec.DisplayName
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

@DisplayName("UNIT - CompareSpec")
class CompareSpec : StringSpec({

    data class Sample(val name: String, val target: Any?, val other: Any?, val expected: Boolean)

    data class X(val x: String)

    val sameInstance = X("SAME")

    @Suppress("BooleanLiteralArgument")
    val examples = listOf(

        //  null  //////////////////////////////////////////////////////////////////////////////////////////////////////

        Sample("Null vs Null", null, null, true),

        //  null vs other
        Sample("Null vs Boolean", null, true, false),
        Sample("Null vs Boolean", null, false, false),
        Sample("Null vs Char", null, 'B', false),
        Sample("Null vs Byte", null, 0.toByte(), false),
        Sample("Null vs Short", null, 0.toShort(), false),
        Sample("Null vs Int", null, 0, false),
        Sample("Null vs Long", null, 0L, false),
        Sample("Null vs Float", null, 0.0f, false),
        Sample("Null vs Double", null, 0.0, false),
        Sample("Null vs Unit", null, Unit, false),
        Sample("Null vs String", null, "null", false),

        //  NaN  ///////////////////////////////////////////////////////////////////////////////////////////////////////

        Sample("NaN - Double.NaN vs Double.NaN", Double.NaN, Double.NaN, true),
        Sample("NaN - Double.NaN vs Float.NaN", Double.NaN, Float.NaN, false),
        Sample("NaN - Double.NaN vs 0.0", Double.NaN, 0.0, false),
        Sample("NaN - Float.NaN vs 0.0f", Float.NaN, 0.0f, false),

        //  data classes  //////////////////////////////////////////////////////////////////////////////////////////////

        Sample("Data-class vs Same instance", sameInstance, sameInstance, true),
        Sample("Data-class vs Other instance", X("SAME"), X("SAME"), false),
        Sample("Data-class vs Other instance", X("SAME"), X("NOT-SAME"), false),

        // data classes vs others
        Sample("Data-class vs Null", X("SAME"), null, false),

        //  string  ////////////////////////////////////////////////////////////////////////////////////////////////////

        Sample("String vs String", "", "", true),
        Sample("String vs String", "a", "", false),
        Sample("String vs String", "S-1", "S-1", true),
        Sample("String vs String", "S-1", "S-2", false),

        // string vs others
        Sample("String vs Null", "", null, false),
        Sample("String vs Null", "S-1", null, false),
        Sample("String vs Boolean", "true", true, false),
        Sample("String vs Char", "S", 'S', false),
        Sample("String vs Byte", "A", 65.toByte(), false),
        Sample("String vs Short", "A", 65.toShort(), false),
        Sample("String vs Int", "A", 65, false),
        Sample("String vs Long", "A", 65L, false),
        Sample("String vs Float", "A", 65.0f, false),
        Sample("String vs Double", "A", 65.0, false),
        Sample("String vs Void", "kotlin.Unit", Unit, false),

        //  booleans  //////////////////////////////////////////////////////////////////////////////////////////////////

        Sample("Boolean vs Boolean", true, true, true),
        Sample("Boolean vs Boolean", false, false, true),
        Sample("Boolean vs Boolean", false, true, false),
        Sample("Boolean vs Boolean", true, false, false),

        // boolean vs others
        Sample("Boolean vs Null", true, null, false),
        Sample("Boolean vs Null", false, null, false),
        Sample("Boolean vs Char", true, '1', false),
        Sample("Boolean vs Char", false, '0', false),
        Sample("Boolean vs Byte", true, 1.toByte(), false),
        Sample("Boolean vs Byte", false, 0.toByte(), false),
        Sample("Boolean vs Short", true, 1.toShort(), false),
        Sample("Boolean vs Short", false, 0.toShort(), false),
        Sample("Boolean vs Int", true, 1, false),
        Sample("Boolean vs Int", false, 0, false),
        Sample("Boolean vs Long", true, 1L, false),
        Sample("Boolean vs Long", false, 0L, false),
        Sample("Boolean vs Float", true, 1.0f, false),
        Sample("Boolean vs Float", false, 0.0f, false),
        Sample("Boolean vs Double", true, 1.0, false),
        Sample("Boolean vs Double", false, 0.0, false),
        Sample("Boolean vs String", false, "false", false),
        Sample("Boolean vs String", true, "true", false),
        Sample("Boolean vs Void", false, Unit, false),
        Sample("Boolean vs Void", true, Unit, false),

        //  chars  /////////////////////////////////////////////////////////////////////////////////////////////////////

        Sample("Char vs Char", 'A', 'A', true),
        Sample("Char vs Char", 'A', 'B', false),

        // char vs others
        Sample("Char vs Null", 'A', null, false),
        Sample("Char vs Boolean", '1', true, false),
        Sample("Char vs Byte", '1', 1.toByte(), false),
        Sample("Char vs Short", '1', 1.toShort(), false),
        Sample("Char vs Int", '1', 1, false),
        Sample("Char vs Long", '1', 1L, false),
        Sample("Char vs Float", '1', 1.0f, false),
        Sample("Char vs Double", '1', 1.0, false),
        Sample("Char vs String", '0', "false", false),
        Sample("Char vs Void", '0', Unit, false),

        //  byte  //////////////////////////////////////////////////////////////////////////////////////////////////////

        Sample("Byte vs Byte", 0.toByte(), 0.toByte(), true),
        Sample("Byte vs Byte", 1.toByte(), 1.toByte(), true),
        Sample("Byte vs Byte", 0.toByte(), 1.toByte(), false),

        // byte vs others
        Sample("Byte vs Null", 0.toByte(), null, false),
        Sample("Byte vs Boolean", 1.toByte(), true, false),
        Sample("Byte vs Char", 1.toByte(), '1', false),
        Sample("Byte vs Short", 1.toByte(), 1.toShort(), false),
        Sample("Byte vs Int", 1.toByte(), 1, false),
        Sample("Byte vs Long", 1.toByte(), 1L, false),
        Sample("Byte vs Float", 1.toByte(), 1.0f, false),
        Sample("Byte vs Double", 1.toByte(), 1.0, false),
        Sample("Byte vs String", 1.toByte(), "false", false),
        Sample("Byte vs Void", 0.toByte(), Unit, false),

        //  short  /////////////////////////////////////////////////////////////////////////////////////////////////////

        Sample("Short vs Short", 0.toShort(), 0.toShort(), true),
        Sample("Short vs Short", 1.toShort(), 1.toShort(), true),
        Sample("Short vs Short", 0.toShort(), 1.toShort(), false),

        // short vs others
        Sample("Short vs Null", 0.toShort(), null, false),
        Sample("Short vs Boolean", 1.toShort(), true, false),
        Sample("Short vs Char", 1.toShort(), '1', false),
        Sample("Short vs Byte", 1.toShort(), 1.toByte(), false),
        Sample("Short vs Int", 1.toShort(), 1, false),
        Sample("Short vs Long", 1.toShort(), 1L, false),
        Sample("Short vs Float", 1.toShort(), 1.0f, false),
        Sample("Short vs Double", 1.toShort(), 1.0, false),
        Sample("Short vs String", 1.toShort(), "false", false),
        Sample("Short vs Void", 0.toShort(), Unit, false),

        //  int  ///////////////////////////////////////////////////////////////////////////////////////////////////////

        Sample("Int vs Int", 0, 0, true),
        Sample("Int vs Int", 1, 1, true),
        Sample("Int vs Int", 0, 1, false),

        // int vs others
        Sample("Int vs Null", 0, null, false),
        Sample("Int vs Boolean", 1, true, false),
        Sample("Int vs Char", 1, '1', false),
        Sample("Int vs Byte", 1, 1.toByte(), false),
        Sample("Int vs Short", 1, 1.toShort(), false),
        Sample("Int vs Long", 1, 1L, false),
        Sample("Int vs Float", 1, 1.0f, false),
        Sample("Int vs Double", 1, 1.0, false),
        Sample("Int vs String", 1, "false", false),
        Sample("Int vs Void", 0, Unit, false),

        //  long  //////////////////////////////////////////////////////////////////////////////////////////////////////

        Sample("Long vs Long", 0L, 0L, true),
        Sample("Long vs Long", 1L, 1L, true),
        Sample("Long vs Long", 0L, 1L, false),

        // long vs others
        Sample("Long vs Null", 0L, null, false),
        Sample("Long vs Boolean", 1L, true, false),
        Sample("Long vs Char", 1L, '1', false),
        Sample("Long vs Byte", 1L, 1.toByte(), false),
        Sample("Long vs Short", 1L, 1.toShort(), false),
        Sample("Long vs Int", 1L, 1, false),
        Sample("Long vs Float", 1L, 1.0f, false),
        Sample("Long vs Double", 1L, 1.0, false),
        Sample("Long vs String", 1L, "false", false),
        Sample("Long vs Void", 0L, Unit, false),

        //  float  /////////////////////////////////////////////////////////////////////////////////////////////////////

        Sample("Float vs Float", 0.0f, 0.0f, true),
        Sample("Float vs Float", 1.0f, 1.0f, true),
        Sample("Float vs Float", 0.0f, 1.0f, false),

        // float vs others
        Sample("Float vs Null", 0.0f, null, false),
        Sample("Float vs Boolean", 1.0f, true, false),
        Sample("Float vs Char", 1.0f, '1', false),
        Sample("Float vs Byte", 1.0f, 1.toByte(), false),
        Sample("Float vs Short", 1.0f, 1.toShort(), false),
        Sample("Float vs Int", 1.0f, 1, false),
        Sample("Float vs Long", 1.0f, 1L, false),
        Sample("Float vs Double", 1.0f, 1.0, false),
        Sample("Float vs String", 1.0f, "false", false),
        Sample("Float vs Void", 0.0f, Unit, false),

        //  double  ////////////////////////////////////////////////////////////////////////////////////////////////////

        Sample("Double vs Double", 0.0, 0.0, true),
        Sample("Double vs Double", 1.0, 1.0, true),
        Sample("Double vs Double", 0.0, 1.0, false),

        // float vs others
        Sample("Double vs Null", 0.0, null, false),
        Sample("Double vs Boolean", 1.0, true, false),
        Sample("Double vs Char", 1.0, '1', false),
        Sample("Double vs Byte", 1.0, 1.toByte(), false),
        Sample("Double vs Short", 1.0, 1.toShort(), false),
        Sample("Double vs Int", 1.0, 1, false),
        Sample("Double vs Long", 1.0, 1L, false),
        Sample("Double vs Float", 1.0, 1.0f, false),
        Sample("Double vs String", 1.0, "false", false),
        Sample("Double vs Void", 0.0, Unit, false),

        //  corner cases  //////////////////////////////////////////////////////////////////////////////////////////////

        Sample("Unit vs Unit", Unit, Unit, true)
    )

    examples.forEach { (name, target, other, expected) ->

        "$name: '$target' vs. '$other' should be ${if (expected) "the same" else "different"}" {

            val resultLtr = target isSameAs other
            val resultRtl = other isSameAs target

            assertSoftly {

                withClue("Should work left to right (target isSameAs other)") {
                    resultLtr shouldBe expected
                }

                withClue("Should work right to left (other isSameAs target)") {
                    resultRtl shouldBe expected
                }
            }
        }
    }
})
