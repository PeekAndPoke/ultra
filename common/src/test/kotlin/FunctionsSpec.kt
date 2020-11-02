package de.peekandpoke.ultra.common

import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class FunctionsSpec : StringSpec({

    "Function.nthParamName: Getting the nth-parameter name must work" {

        val subject1 = { p1: Int, p2: Int, p3: Int -> p1 + p2 + p3 }

        val subject2 = { x1: Int, x2: Int, x3: Int -> x1 + x2 + x3 }

        assertSoftly {

            subject1.nthParamName(0) shouldBe "p1"
            subject1.nthParamName(1) shouldBe "p2"
            subject1.nthParamName(2) shouldBe "p3"

            subject2.nthParamName(0) shouldBe "x1"
            subject2.nthParamName(1) shouldBe "x2"
            subject2.nthParamName(2) shouldBe "x3"

            // getting the param names again... this makes sure that the internal cache is working properly

            subject1.nthParamName(0) shouldBe "p1"
            subject1.nthParamName(1) shouldBe "p2"
            subject1.nthParamName(2) shouldBe "p3"

            subject2.nthParamName(0) shouldBe "x1"
            subject2.nthParamName(1) shouldBe "x2"
            subject2.nthParamName(2) shouldBe "x3"
        }
    }

    "Function.nthParamName: Getting the first param name which is 'it'" {

        val subject: Function1<Int, Any> = { it }

        assertSoftly {
            subject.nthParamName(0) shouldBe "it"
        }
    }

    "Function.nthParamName: Getting the name of an ignored parameter" {

        val subject: Function1<Int, Any?> = { _ -> null }

        assertSoftly {
            subject.nthParamName(0) shouldBe "param0"
        }
    }
})
