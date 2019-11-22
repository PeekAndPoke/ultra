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
            generic = Generic(one = "one", two = 2),
            generic2 = Generic(one = "one", two = listOf(1, 2, 3, 4))
        )

        val result = source.mutate {
            generic.one += "!"
            generic.two += 1

            generic2.one += "!!"
            generic2.two.retainWhere { this % 2 != 0 }
        }

        assertSoftly {
            result.generic.one shouldBe "one!"
            result.generic.two shouldBe 3

            result.generic2.one shouldBe "one!!"
            result.generic2.two shouldBe listOf(1, 3)
        }
    }

    "Mutating a class that references a generic class within another generic" {

        val source = GenericTypeInActionWithContainers(
            generic = listOf(
                Generic(one = "one" as Any, two = 2)
            ),
            generic2 = mapOf(
                "first" to Generic(one = "one", two = listOf(1, 2, 3, 4) as Any)
            )
        )

        val result = source.mutate {
            with(generic[0]) {
                one = "a"
                two += 1
            }

            with(generic2["first"]!!) {
                one += "!!"
                two = "b"
            }
        }

        assertSoftly {
            result.generic[0].one shouldBe "a"
            result.generic[0].two shouldBe 3

            result.generic2["first"]?.one shouldBe "one!!"
            result.generic2["first"]?.two shouldBe "b"
        }
    }
})
