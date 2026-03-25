package io.peekandpoke.ultra.kontainer.e2e

import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.kontainer.KontainerBlueprint
import io.peekandpoke.ultra.kontainer.ServiceProvider
import io.peekandpoke.ultra.kontainer.kontainer

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
            }.create()

            assertSoftly {
                subject.getProvider(Prototype::class).type shouldBe ServiceProvider.Type.Prototype
                subject.getProvider(Dynamic::class).type shouldBe ServiceProvider.Type.Dynamic

                subject.get(Prototype::class).dynamic::class shouldBe Dynamic::class
            }
        }
    }
}
