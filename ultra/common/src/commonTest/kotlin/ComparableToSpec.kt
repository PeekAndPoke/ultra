package io.peekandpoke.ultra.common

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

private data class Score(val value: Int) : ComparableTo<Score> {
    override fun compareTo(other: Score): Int = value.compareTo(other.value)
}

class ComparableToSpec : StringSpec({

    "isGreaterThan" {
        (Score(10) isGreaterThan Score(5)) shouldBe true
        (Score(5) isGreaterThan Score(10)) shouldBe false
        (Score(5) isGreaterThan Score(5)) shouldBe false
    }

    "isGreaterThanOrEqualTo" {
        (Score(10) isGreaterThanOrEqualTo Score(5)) shouldBe true
        (Score(5) isGreaterThanOrEqualTo Score(5)) shouldBe true
        (Score(5) isGreaterThanOrEqualTo Score(10)) shouldBe false
    }

    "isEqualTo" {
        (Score(5) isEqualTo Score(5)) shouldBe true
        (Score(5) isEqualTo Score(10)) shouldBe false
    }

    "isLessThanOrEqualTo" {
        (Score(5) isLessThanOrEqualTo Score(10)) shouldBe true
        (Score(5) isLessThanOrEqualTo Score(5)) shouldBe true
        (Score(10) isLessThanOrEqualTo Score(5)) shouldBe false
    }

    "isLessThan" {
        (Score(5) isLessThan Score(10)) shouldBe true
        (Score(10) isLessThan Score(5)) shouldBe false
        (Score(5) isLessThan Score(5)) shouldBe false
    }
})
