package de.peekandpoke.ultra.common

import io.kotlintest.assertSoftly
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec

class SimpleLazySpec : StringSpec({

    "General functionality" {

        var counter = 0

        val subject = SimpleLazy {
            counter++

            "returned value"
        }

        assertSoftly {
            // value not yet retrieved
            counter shouldBe 0
            subject.isInitialized() shouldBe false

            // creating the value
            subject.value shouldBe "returned value"
            subject.isInitialized() shouldBe true
            counter shouldBe 1

            // returning the already created value
            subject.value shouldBe "returned value"
            subject.isInitialized() shouldBe true
            counter shouldBe 1
        }
    }
})
