package de.peekandpoke.ultra.kontainer.e2e

import de.peekandpoke.ultra.kontainer.AnotherInjectingService
import de.peekandpoke.ultra.kontainer.AnotherSimpleService
import de.peekandpoke.ultra.kontainer.CounterService
import de.peekandpoke.ultra.kontainer.CounterServiceEx01
import de.peekandpoke.ultra.kontainer.ServiceProvider
import de.peekandpoke.ultra.kontainer.SomeIndependentService
import de.peekandpoke.ultra.kontainer.kontainer
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs

class ServiceProviderFactorySpec : StringSpec({

    "Service providers must only be created on demand" {

        val kontainer = kontainer {
            instance(AnotherSimpleService())
            singleton(CounterService::class)
            dynamic(SomeIndependentService::class)
        }.create()

        val factory = kontainer.getFactory()

        assertSoftly {
            factory.isProviderCreated(AnotherSimpleService::class) shouldBe false
            factory.isProviderCreated(CounterService::class) shouldBe false
            factory.isProviderCreated(SomeIndependentService::class) shouldBe false

            kontainer.get(AnotherSimpleService::class)

            factory.isProviderCreated(AnotherSimpleService::class) shouldBe true
            factory.isProviderCreated(CounterService::class) shouldBe false
            factory.isProviderCreated(SomeIndependentService::class) shouldBe false

            kontainer.get(SomeIndependentService::class)

            factory.isProviderCreated(AnotherSimpleService::class) shouldBe true
            factory.isProviderCreated(CounterService::class) shouldBe false
            factory.isProviderCreated(SomeIndependentService::class) shouldBe true

            kontainer.get(CounterService::class)

            factory.isProviderCreated(AnotherSimpleService::class) shouldBe true
            factory.isProviderCreated(CounterService::class) shouldBe true
            factory.isProviderCreated(SomeIndependentService::class) shouldBe true
        }
    }

    "Instance service providers stay the same instance for a kontainer instance" {

        val blueprint = kontainer {
            instance(CounterService())
        }

        val kontainer = blueprint.create()

        kontainer.getFactory().getProvider(CounterService::class) shouldBeSameInstanceAs
                kontainer.getFactory().getProvider(CounterService::class)

        kontainer.getFactory().getProvider(CounterService::class).type shouldBe ServiceProvider.Type.Singleton
    }

    "Instance service providers must be shared across kontainer instances" {

        val blueprint = kontainer {
            instance(CounterService())
        }

        val first = blueprint.create()
        val second = blueprint.create()

        assertSoftly {

            withClue("Initially no providers must be created") {
                first.getFactory().isProviderCreated(CounterService::class) shouldBe false
                second.getFactory().isProviderCreated(CounterService::class) shouldBe false
            }

            withClue("Both container must use the same provider") {
                first.getFactory().getProvider(CounterService::class) shouldBeSameInstanceAs
                        second.getFactory().getProvider(CounterService::class)
            }
        }
    }

    "Singleton service providers stay the same instance for a kontainer instance" {

        val blueprint = kontainer {
            singleton(CounterService::class)
        }

        val kontainer = blueprint.create()

        kontainer.getFactory().getProvider(CounterService::class) shouldBeSameInstanceAs
                kontainer.getFactory().getProvider(CounterService::class)

        kontainer.getFactory().getProvider(CounterService::class).type shouldBe ServiceProvider.Type.Singleton
    }

    "Singleton service providers must be shared across kontainer instances" {

        val blueprint = kontainer {
            singleton(CounterService::class)
        }

        val first = blueprint.create()
        val second = blueprint.create()

        assertSoftly {

            withClue("Initially no providers must be created") {
                first.getFactory().isProviderCreated(CounterService::class) shouldBe false
                second.getFactory().isProviderCreated(CounterService::class) shouldBe false
            }

            withClue("Both container must use the same provider") {
                first.getFactory().getProvider(CounterService::class) shouldBeSameInstanceAs
                        second.getFactory().getProvider(CounterService::class)
            }
        }
    }

    "Semi Dynamic service providers stay the same instance for a kontainer instance" {

        val blueprint = kontainer {
            dynamic(AnotherSimpleService::class)
            singleton(AnotherInjectingService::class)
        }

        val kontainer = blueprint.create()

        kontainer.getFactory().getProvider(AnotherInjectingService::class) shouldBeSameInstanceAs
                kontainer.getFactory().getProvider(AnotherInjectingService::class)

        kontainer.getFactory().getProvider(AnotherInjectingService::class).type shouldBe
                ServiceProvider.Type.SemiDynamic
    }

    "Semi Dynamic service providers must NOT be shared across kontainer instances" {

        val blueprint = kontainer {
            dynamic(AnotherSimpleService::class)
            singleton(AnotherInjectingService::class)
        }

        val first = blueprint.create()
        val second = blueprint.create()

        assertSoftly {

            withClue("Initially no providers must be created") {
                first.getFactory().isProviderCreated(AnotherInjectingService::class) shouldBe false
                second.getFactory().isProviderCreated(AnotherInjectingService::class) shouldBe false
            }

            withClue("Both container must use different providers") {
                first.getFactory().getProvider(AnotherInjectingService::class) shouldNotBeSameInstanceAs
                        second.getFactory().getProvider(AnotherInjectingService::class)
            }
        }
    }

    "Dynamic service providers stay the same instance for a kontainer instance" {

        val blueprint = kontainer {
            dynamic(CounterService::class)
        }

        val kontainer = blueprint.create()

        kontainer.getFactory().getProvider(CounterService::class) shouldBeSameInstanceAs
                kontainer.getFactory().getProvider(CounterService::class)

        kontainer.getFactory().getProvider(CounterService::class).type shouldBe ServiceProvider.Type.Dynamic
    }

    "Dynamic service providers must NOT be shared across kontainer instances" {

        val blueprint = kontainer {
            dynamic(CounterService::class)
        }

        val first = blueprint.create()
        val second = blueprint.create()

        assertSoftly {

            withClue("Initially no providers must be created") {
                first.getFactory().isProviderCreated(CounterService::class) shouldBe false
                second.getFactory().isProviderCreated(CounterService::class) shouldBe false
            }

            withClue("Both container must use different providers") {
                first.getFactory().getProvider(CounterService::class) shouldNotBeSameInstanceAs
                        second.getFactory().getProvider(CounterService::class)
            }
        }
    }

    "Overwritten Dynamic service providers stay the same instance for a kontainer instance" {

        val blueprint = kontainer {
            dynamic(CounterService::class)
        }

        val kontainer = blueprint.create {
            with { CounterServiceEx01() }
        }

        kontainer.getFactory().getProvider(CounterService::class) shouldBeSameInstanceAs
                kontainer.getFactory().getProvider(CounterService::class)

        kontainer.getFactory().getProvider(CounterService::class).type shouldBe ServiceProvider.Type.DynamicOverride

        kontainer.get(CounterService::class).shouldBeInstanceOf<CounterServiceEx01>()
    }

    "Overwritten Dynamic service providers must NOT be shared across kontainer instances" {

        val blueprint = kontainer {
            dynamic(CounterService::class)
        }

        val first = blueprint.create {
            with { CounterServiceEx01() }
        }

        val second = blueprint.create {
            with { CounterServiceEx01() }
        }

        assertSoftly {

            withClue("Initially no providers must be created") {
                first.getFactory().isProviderCreated(CounterService::class) shouldBe false
                second.getFactory().isProviderCreated(CounterService::class) shouldBe false
            }

            withClue("Both container must use different providers") {
                first.getFactory().getProvider(CounterService::class) shouldNotBeSameInstanceAs
                        second.getFactory().getProvider(CounterService::class)
            }

            withClue("The services must be different instances") {
                first.get(CounterService::class) shouldNotBeSameInstanceAs
                        second.get(CounterService::class)
            }
        }
    }
})
