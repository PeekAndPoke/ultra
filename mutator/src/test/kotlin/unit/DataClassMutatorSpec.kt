package de.peekandpoke.ultra.mutator.unit

import de.peekandpoke.ultra.mutator.DataClassMutator
import io.kotlintest.DisplayName
import io.kotlintest.assertSoftly
import io.kotlintest.matchers.types.shouldBeSameInstanceAs
import io.kotlintest.matchers.types.shouldNotBeSameInstanceAs
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec

@DisplayName("UNIT - DataClassMutatorSpec")
class DataClassMutatorSpec : StringSpec({

    val source = SomeDataClass(aString = "string", anInt = 1)

    "Creating a data class mutator must not trigger mutation" {

        var timesCloned = 0
        val subject = DataClassMutator(source, { timesCloned++ })

        assertSoftly {

            source shouldBeSameInstanceAs subject.getResult()
            subject.isModified() shouldBe false
            timesCloned shouldBe 0
        }
    }

    "Setting identical values must not trigger mutation" {

        var timesCloned = 0
        val subject = DataClassMutator(source, { timesCloned++ })

        /** @see CompareSpec for details of how values are tested for equality */

        subject.modify(source::aString, source.aString, source.aString + "")
        subject.modify(source::anInt, source.anInt, source.anInt + 0)

        assertSoftly {

            source shouldBeSameInstanceAs subject.getResult()
            subject.isModified() shouldBe false
            timesCloned shouldBe 0
        }
    }

    "Applying a change must trigger mutation" {

        var timesCloned = 0
        val subject = DataClassMutator(source, { timesCloned++ })

        subject.modify(source::aString, source.aString, source.aString + "changed")

        assertSoftly {

            source shouldNotBeSameInstanceAs subject.getResult()
            subject.isModified() shouldBe true
            timesCloned shouldBe 1
        }
    }

    "Only the first change must clone the source" {

        var timesCloned = 0
        val subject = DataClassMutator(source, { timesCloned++ })

        subject.modify(source::aString, source.aString, source.aString + "changed")

        val afterFirstChange = subject.getResult()

        subject.modify(source::aString, source.aString, source.aString + "changed-again")

        val afterSecondChange = subject.getResult()

        assertSoftly {

            source shouldNotBeSameInstanceAs afterFirstChange
            afterFirstChange shouldBeSameInstanceAs afterSecondChange
            subject.isModified() shouldBe true
            timesCloned shouldBe 1
        }
    }
})
