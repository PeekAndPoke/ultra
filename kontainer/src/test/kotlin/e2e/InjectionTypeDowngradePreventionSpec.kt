package de.peekandpoke.ultra.kontainer.e2e

import de.peekandpoke.ultra.kontainer.KontainerBlueprint
import de.peekandpoke.ultra.kontainer.ServiceProvider
import de.peekandpoke.ultra.kontainer.kontainer
import io.kotlintest.assertSoftly
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec

/**
 * See [KontainerBlueprint.semiDynamicDefinitions]
 */
class InjectionTypeDowngradePreventionSpec : StringSpec() {

    class Dynamic

    class Prototype(val dynamic: Dynamic)

    init {
        "A Prototype service must not be downgraded to a semi dynamic singleton when injecting dynamic services" {

            val subject = kontainer {
                dynamic(Dynamic::class)
                prototype(Prototype::class)
            }.useWith()

            assertSoftly {
                subject.getProvider(Prototype::class).type shouldBe ServiceProvider.Type.Prototype
                subject.getProvider(Dynamic::class).type shouldBe ServiceProvider.Type.Dynamic

                subject.get(Prototype::class).dynamic::class shouldBe Dynamic::class
            }
        }
    }
}
