package de.peekandpoke.ultra.mutator.unit

import de.peekandpoke.ultra.mutator.DataClassMutator
import io.kotlintest.DisplayName
import io.kotlintest.assertSoftly
import io.kotlintest.matchers.withClue
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec

@DisplayName("UNIT - DataClassMutatorSpec")
class DataClassMutatorSpec : StringSpec({

    "General behaviour and mutation triggers" {

        var modifications = 0

        val source = SomeDataClass(aString = "string", anInt = 1)
        val subject = DataClassMutator(source, { modifications++ })

        assertSoftly {

            withClue("Creating a data class mutator must not trigger mutation") {

                (source === subject.getResult()) shouldBe true
                modifications shouldBe 0
            }

            withClue("Mutating properties but setting identical values must return the source") {

                subject.modify(source::aString, source.aString, source.aString)
                subject.modify(source::anInt, source.anInt, source.anInt)

                (source === subject.getResult()) shouldBe true
                modifications shouldBe 0
            }

            withClue("Changing a property must trigger mutation") {

                subject.modify(source::aString, source.aString, "changed")

                (source !== subject.getResult()) shouldBe true
                modifications shouldBe 1
            }
        }
    }
})
