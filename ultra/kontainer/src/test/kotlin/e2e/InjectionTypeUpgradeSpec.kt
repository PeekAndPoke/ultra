package io.peekandpoke.ultra.kontainer.e2e

import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.kontainer.KontainerBuilder
import io.peekandpoke.ultra.kontainer.kontainer

/**
 * See [KontainerBuilder.RedefinitionUpgrade]
 */
class InjectionTypeUpgradeSpec : StringSpec() {

    class Service

    init {

        "A Singleton service must be upgraded to a Dynamic when it is overridden" {

            val blueprint = kontainer {
                // First we define the service as a singleton
                singleton(Service::class)
                // Then we re-define the service as a dynamic
                dynamic(Service::class)
            }

            val subject = blueprint.create()

            assertSoftly {
                subject.getProvider(Service::class).reported shouldBe Reported.DynamicSingleton
            }
        }

        "A Singleton service must be upgraded to a Prototype when it is overridden" {

            val blueprint = kontainer {
                // First we define the service as a singleton
                singleton(Service::class)
                // Then we re-define the service as a prototype
                prototype(Service::class)
            }

            val subject = blueprint.create()

            assertSoftly {
                subject.getProvider(Service::class).reported shouldBe Reported.StaticPrototype
            }
        }

        "A Dynamic service must NOT be converted to a Prototype when it is overridden" {

            val blueprint = kontainer {
                // First we define the service as a dynamic
                dynamic(Service::class)
                // Then we re-define the service as a prototype
                prototype(Service::class)
            }

            val subject = blueprint.create()

            assertSoftly {
                subject.getProvider(Service::class).reported shouldBe Reported.DynamicSingleton
            }
        }

        "A Dynamic service must not be downgraded to a Singleton when it is overridden" {

            val blueprint = kontainer {
                // First we define the service as a dynamic
                dynamic(Service::class)
                // Then we re-define the service as a singleton
                singleton(Service::class)
            }

            val subject = blueprint.create()

            assertSoftly {
                subject.getProvider(Service::class).reported shouldBe Reported.DynamicSingleton
            }
        }

        "A Prototype service must not be downgraded to a singleton when it is overridden" {

            val blueprint = kontainer {
                // First we define the service as a prototype
                prototype(Service::class)
                // Then we re-define the service as a singleton
                singleton(Service::class)
            }

            val subject = blueprint.create()

            assertSoftly {
                subject.getProvider(Service::class).reported shouldBe Reported.StaticPrototype
            }
        }

        "A Prototype service redefined as dynamic stays a Prototype and becomes Dynamic (ULTRA-00331)" {

            val blueprint = kontainer {
                // First we define the service as a prototype
                prototype(Service::class)
                // Then we re-define the service as a dynamic
                dynamic(Service::class)
            }

            val subject = blueprint.create()

            assertSoftly {
                subject.getProvider(Service::class).reported shouldBe Reported.DynamicPrototype
            }
        }
    }
}
