package de.peekandpoke.ultra.common

import io.kotlintest.assertSoftly
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import io.kotlintest.specs.StringSpec

class SimpleLookupSpec : StringSpec({

    "General functionality" {

        class A
        class B
        class C

        val a = A()
        val b = B()
        val a2 = A()

        val subject = SimpleLookup { listOf(a, b, a2) }

        assertSoftly {
            shouldThrow<IllegalStateException> {
                subject.get(C::class)
            }

            subject.getOrNull(C::class) shouldBe null

            subject.get(A::class) shouldBe a2
            subject.getOrNull(A::class) shouldBe a2

            subject.get(B::class) shouldBe b
            subject.getOrNull(B::class) shouldBe b
        }
    }

})
