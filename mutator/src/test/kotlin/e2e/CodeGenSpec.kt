package de.peekandpoke.ultra.mutator.e2e

import de.peekandpoke.ultra.mutator.e2e.package1.FirstInPackage1
import de.peekandpoke.ultra.mutator.e2e.package2.FirstInPackage2
import io.kotlintest.DisplayName
import io.kotlintest.assertSoftly
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import io.kotlintest.specs.StringSpec

@DisplayName("E2E - CodeGenSpec")
class CodeGenSpec : StringSpec({

    "Generated code must contain imports to referenced classes in other packages" {

        val source = ReferencingOtherPackages(
            firstIn1 = FirstInPackage1("firstIn1"),
            secondIn1 = null,
            firstIn2 = FirstInPackage2("firstIn2"),
            secondIn2 = null
        )

        val result = source.mutate {
            firstIn1.name = "firstIn1 modified"

            firstIn2 += FirstInPackage2("firstIn2")
        }

        assertSoftly {

            result.firstIn1.name shouldBe "firstIn1 modified"

            result.firstIn2 shouldNotBe null
        }
    }

    "Generated code must contain correct imports for nested classes" {

        val source = WithNestedClass(
            nested = WithNestedClass.InnerClass(
                text = "hello"
            )
        )

        val result = source.mutate {
            nested.text = "bye"
        }

        assertSoftly {
            result.nested.text shouldBe "bye"
        }
    }
})
