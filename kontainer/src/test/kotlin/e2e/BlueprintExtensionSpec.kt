package de.peekandpoke.ultra.kontainer.e2e

import de.peekandpoke.ultra.kontainer.kontainer
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldNotBeSameInstanceAs

class BlueprintExtensionSpec : FreeSpec() {

    init {
        "Extending an existing KontainerBlueprint must work" {

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

            val kontainerOne = extensionOne.create()

            val extensionTwo = blueprint.extend {
                singleton(MyImplTwo::class)
            }

            val kontainerTwo = extensionTwo.create()

            assertSoftly {
                withClue("Every kontainer must create its own singletons instances") {
                    kontainerOne.get(MyService::class) shouldNotBeSameInstanceAs kontainerTwo.get(MyService::class)
                }

                withClue("each kontainer must inject the correct services") {
                    kontainerOne.get(MyService::class).injects::class shouldBe MyImplOne::class

                    kontainerTwo.get(MyService::class).injects::class shouldBe MyImplTwo::class
                }
            }
        }
    }
}
