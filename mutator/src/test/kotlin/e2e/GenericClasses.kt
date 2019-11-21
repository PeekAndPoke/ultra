package de.peekandpoke.ultra.mutator.e2e

import de.peekandpoke.ultra.mutator.e2e.package1.FirstInPackage1
import de.peekandpoke.ultra.mutator.e2e.package2.FirstInPackage2
import io.kotlintest.assertSoftly
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec

class GenericClasses : StringSpec({

    "Mutating a class that references a generic class (objects)" {

        val source = GenericTypeInAction(
            generic1 = Generic(
                one = FirstInPackage1("one"),
                two = FirstInPackage2("two")
            ),
            generic2 = Generic(
                one = FirstInPackage2("three"),
                two = Generic(
                    one = FirstInPackage1("four"),
                    two = FirstInPackage2("five")
                )
            )
        )

        val result = source.mutate {
            generic1.one.name += "!"
            generic1.two.name += "!"

            generic2.one.name += "!"

            generic2.two.one.name += "!"
            generic2.two.two.name += "!"
        }

        assertSoftly {
            result.generic1.one.name shouldBe "one!"
            result.generic1.two.name shouldBe "two!"

            result.generic2.one.name shouldBe "three!"

            result.generic2.two.one.name shouldBe "four!"
            result.generic2.two.two.name shouldBe "five!"
        }
    }

    "Mutating a class that references a generic class (primitives)" {

        val source = GenericTypeInActionWithPrimitives(
            generic = Generic(
                one = "one",
                two = 2
            )
        )

        val result = source.mutate {
            generic.one += "!"
            generic.two += 1
        }

        assertSoftly {
            result.generic.one shouldBe "one!"
            result.generic.two shouldBe 3
        }
    }
})
