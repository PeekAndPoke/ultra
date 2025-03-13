package de.peekandpoke.ultra.mutator.unit

import de.peekandpoke.ultra.mutator.Cloner
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ClonerSpec : StringSpec({

    "Cloning a class with no-arg primary constructor" {

        class Data

        val result = Cloner.cloneDataClass(Data())

        assertSoftly {
            result::class shouldBe Data::class
        }
    }

    // TODO: tests for cloning data classes
})
