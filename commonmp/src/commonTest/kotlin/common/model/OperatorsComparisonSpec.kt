package de.peekandpoke.ultra.common.model

import de.peekandpoke.ultra.common.ComparableTo
import de.peekandpoke.ultra.common.model.Operators.Comparison
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class OperatorsComparisonSpec : StringSpec({

    "Must work for simple integers" {

        Comparison.LT(1, 2) shouldBe true
        Comparison.LT(2, 2) shouldBe false
        Comparison.LT(3, 2) shouldBe false

        Comparison.LTE(1, 2) shouldBe true
        Comparison.LTE(2, 2) shouldBe true
        Comparison.LTE(3, 2) shouldBe false

        Comparison.EQ(1, 2) shouldBe false
        Comparison.EQ(2, 2) shouldBe true
        Comparison.EQ(3, 2) shouldBe false

        Comparison.GTE(1, 2) shouldBe false
        Comparison.GTE(2, 2) shouldBe true
        Comparison.GTE(3, 2) shouldBe true

        Comparison.GT(1, 2) shouldBe false
        Comparison.GT(2, 2) shouldBe false
        Comparison.GT(3, 2) shouldBe true
    }

    "Must work for a simple data class" {

        data class Val(val v: Int) : ComparableTo<Int> {
            override fun compareTo(other: Int): Int {
                return v.compareTo(other)
            }
        }

        Comparison.LT(Val(1), 2) shouldBe true
        Comparison.LT(Val(2), 2) shouldBe false
        Comparison.LT(Val(3), 2) shouldBe false

        Comparison.LTE(Val(1), 2) shouldBe true
        Comparison.LTE(Val(2), 2) shouldBe true
        Comparison.LTE(Val(3), 2) shouldBe false

        Comparison.EQ(Val(1), 2) shouldBe false
        Comparison.EQ(Val(2), 2) shouldBe true
        Comparison.EQ(Val(3), 2) shouldBe false

        Comparison.GTE(Val(1), 2) shouldBe false
        Comparison.GTE(Val(2), 2) shouldBe true
        Comparison.GTE(Val(3), 2) shouldBe true

        Comparison.GT(Val(1), 2) shouldBe false
        Comparison.GT(Val(2), 2) shouldBe false
        Comparison.GT(Val(3), 2) shouldBe true
    }

    "toHumanString()" {
        Comparison.LT.toHumanString() shouldBe "<"
        Comparison.LTE.toHumanString() shouldBe "<="
        Comparison.EQ.toHumanString() shouldBe "=="
        Comparison.GTE.toHumanString() shouldBe ">="
        Comparison.GT.toHumanString() shouldBe ">"
    }
})
