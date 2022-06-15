package de.peekandpoke.ultra.common.model

import de.peekandpoke.ultra.common.ComparableTo
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class OperatorsComparisonSpec : StringSpec({

    "Must work for simple integers" {

        Operators.Comparison.LT(1, 2) shouldBe true
        Operators.Comparison.LT(2, 2) shouldBe false
        Operators.Comparison.LT(3, 2) shouldBe false

        Operators.Comparison.LTE(1, 2) shouldBe true
        Operators.Comparison.LTE(2, 2) shouldBe true
        Operators.Comparison.LTE(3, 2) shouldBe false

        Operators.Comparison.EQ(1, 2) shouldBe false
        Operators.Comparison.EQ(2, 2) shouldBe true
        Operators.Comparison.EQ(3, 2) shouldBe false

        Operators.Comparison.GTE(1, 2) shouldBe false
        Operators.Comparison.GTE(2, 2) shouldBe true
        Operators.Comparison.GTE(3, 2) shouldBe true

        Operators.Comparison.GT(1, 2) shouldBe false
        Operators.Comparison.GT(2, 2) shouldBe false
        Operators.Comparison.GT(3, 2) shouldBe true
    }

    "Must work for a simple data class" {

        data class Val(val v: Int) : ComparableTo<Int> {
            override fun compareTo(other: Int): Int {
                return v.compareTo(other)
            }
        }

        Operators.Comparison.LT(Val(1), 2) shouldBe true
        Operators.Comparison.LT(Val(2), 2) shouldBe false
        Operators.Comparison.LT(Val(3), 2) shouldBe false

        Operators.Comparison.LTE(Val(1), 2) shouldBe true
        Operators.Comparison.LTE(Val(2), 2) shouldBe true
        Operators.Comparison.LTE(Val(3), 2) shouldBe false

        Operators.Comparison.EQ(Val(1), 2) shouldBe false
        Operators.Comparison.EQ(Val(2), 2) shouldBe true
        Operators.Comparison.EQ(Val(3), 2) shouldBe false

        Operators.Comparison.GTE(Val(1), 2) shouldBe false
        Operators.Comparison.GTE(Val(2), 2) shouldBe true
        Operators.Comparison.GTE(Val(3), 2) shouldBe true

        Operators.Comparison.GT(Val(1), 2) shouldBe false
        Operators.Comparison.GT(Val(2), 2) shouldBe false
        Operators.Comparison.GT(Val(3), 2) shouldBe true
    }
})
