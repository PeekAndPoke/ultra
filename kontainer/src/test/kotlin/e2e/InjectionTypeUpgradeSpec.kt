package de.peekandpoke.ultra.kontainer.e2e

import de.peekandpoke.ultra.kontainer.KontainerBuilder
import de.peekandpoke.ultra.kontainer.ServiceProvider
import de.peekandpoke.ultra.kontainer.kontainer
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

/**
 * See [KontainerBuilder.InjectionTypeUpgrade]
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
                subject.getProvider(Service::class).type shouldBe ServiceProvider.Type.Dynamic
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
                subject.getProvider(Service::class).type shouldBe ServiceProvider.Type.Prototype
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
                subject.getProvider(Service::class).type shouldBe ServiceProvider.Type.Dynamic
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
                subject.getProvider(Service::class).type shouldBe ServiceProvider.Type.Dynamic
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
                subject.getProvider(Service::class).type shouldBe ServiceProvider.Type.Prototype
            }
        }

        "A Prototype service must not be downgraded to a dynamic when it is overridden" {

            val blueprint = kontainer {
                // First we define the service as a prototype
                prototype(Service::class)
                // Then we re-define the service as a dynamic
                dynamic(Service::class)
            }

            val subject = blueprint.create()

            assertSoftly {
                subject.getProvider(Service::class).type shouldBe ServiceProvider.Type.Prototype
            }
        }
    }
}
