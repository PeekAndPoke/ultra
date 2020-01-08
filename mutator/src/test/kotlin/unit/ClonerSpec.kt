package de.peekandpoke.ultra.mutator.unit

import de.peekandpoke.ultra.mutator.Cloner
import io.kotlintest.assertSoftly
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec

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
