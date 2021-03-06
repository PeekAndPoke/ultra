package de.peekandpoke.ultra.kontainer.e2e

import de.peekandpoke.ultra.kontainer.kontainer
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldNotBeSameInstanceAs

class BlueprintExtensionSpec : StringSpec({

    "Extending an existing KontainerBlueprint" {

        abstract class MyBase
        class MyImplOne : MyBase()
        class MyImplTwo : MyBase()

        class MyService(val injects: MyBase)

        val blueprint = kontainer {
            singleton(MyService::class)
        }

        val extensionOne = blueprint.extend {
            singleton(MyImplOne::class)
        }

        val kontainerOne = extensionOne.useWith()

        val extensionTwo = blueprint.extend {
            singleton(MyImplTwo::class)
        }

        val kontainerTwo = extensionTwo.useWith()

        assertSoftly {
            withClue("each kontainer must instantiate its own singletons") {
                kontainerOne.get(MyService::class) shouldNotBeSameInstanceAs kontainerTwo.get(MyService::class)
            }

            withClue("each kontainer must inject the correct services") {
                kontainerOne.get(MyService::class).injects::class shouldBe MyImplOne::class

                kontainerTwo.get(MyService::class).injects::class shouldBe MyImplTwo::class
            }
        }
    }
})
