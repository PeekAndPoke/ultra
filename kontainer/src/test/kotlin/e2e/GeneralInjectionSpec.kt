package de.peekandpoke.ultra.kontainer.e2e

import de.peekandpoke.ultra.kontainer.AnotherInjectingService
import de.peekandpoke.ultra.kontainer.AnotherSimpleService
import de.peekandpoke.ultra.kontainer.CounterService
import de.peekandpoke.ultra.kontainer.CounterServiceEx01
import de.peekandpoke.ultra.kontainer.CounterServiceEx02
import de.peekandpoke.ultra.kontainer.DeeperInjectingService
import de.peekandpoke.ultra.kontainer.InjectingService
import de.peekandpoke.ultra.kontainer.InjectingSomethingWeird
import de.peekandpoke.ultra.kontainer.Kontainer
import de.peekandpoke.ultra.kontainer.KontainerBlueprint
import de.peekandpoke.ultra.kontainer.KontainerInconsistent
import de.peekandpoke.ultra.kontainer.ServiceNotFound
import de.peekandpoke.ultra.kontainer.ServiceProvider
import de.peekandpoke.ultra.kontainer.kontainer
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs

class GeneralInjectionSpec : StringSpec({

    "Creating an empty container" {

        val blueprint = kontainer {}

        val subject = blueprint.create()

        subject shouldNotBe null
    }

    "Requesting a singleton that is not registered must throw [ServiceNotFound]" {

        val result = kontainer { }.create()

        shouldThrow<ServiceNotFound> {
            result.get<CounterService>()
        }
    }

    "Singleton services must be provided correctly" {

        val subject = kontainer {
            singleton(CounterService::class)
        }.create()

        assertSoftly {
            withClue("The service must be created") {
                subject.get(CounterService::class)::class shouldBe CounterService::class
            }

            withClue("Requesting the same service twice must return the same instance") {
                subject.get(CounterService::class) shouldBeSameInstanceAs subject.get(CounterService::class)
            }

            withClue("The service type must be correct") {
                subject.getProvider<CounterService>().type shouldBe ServiceProvider.Type.Singleton
            }
        }
    }

    "Container with a singleton service (builder style)" {

        val subject = kontainer {
            singleton0(CounterService::class) { CounterService() }
        }.create()

        assertSoftly {

            subject.get<CounterService>()::class shouldBe CounterService::class
            subject.getProvider<CounterService>().type shouldBe ServiceProvider.Type.Singleton
        }
    }

    "Instance singletons must be provided correctly" {

        val subject = kontainer {
            instance(CounterService())
        }.create()

        assertSoftly {

            subject.get<CounterService>() shouldBeSameInstanceAs subject.get(CounterService::class)

            subject.get<CounterService>()::class shouldBe CounterService::class
            subject.getProvider<CounterService>().type shouldBe ServiceProvider.Type.Singleton
        }
    }

    "Instance singletons must be shared across container instances created from the same blueprint" {

        val blueprint = kontainer {
            instance(CounterService())
        }

        val first = blueprint.create()
        val second = blueprint.create()

        assertSoftly {
            first.get<CounterService>() shouldBeSameInstanceAs second.get(CounterService::class)
        }
    }

    "Container with a singleton service from existing instance registered with base class" {

        val subject = kontainer {
            instance(CounterService::class, CounterServiceEx01())
        }.create()

        assertSoftly {

            shouldThrow<ServiceNotFound> {
                subject.get<CounterServiceEx01>()
            }

            subject.get<CounterService>() shouldBeSameInstanceAs subject.get(CounterService::class)

            subject.get<CounterService>()::class shouldBe CounterServiceEx01::class
            subject.getProvider<CounterService>().type shouldBe ServiceProvider.Type.Singleton
        }
    }

    "Container with multiple singleton services" {

        val subject = kontainer {
            singleton(CounterService::class)
            singleton(AnotherSimpleService::class)
        }.create()

        assertSoftly {
            subject.get<CounterService>()::class shouldBe CounterService::class
            subject.getProvider<CounterService>().type shouldBe ServiceProvider.Type.Singleton

            subject.get<AnotherSimpleService>()::class shouldBe AnotherSimpleService::class
            subject.getProvider<AnotherSimpleService>().type shouldBe ServiceProvider.Type.Singleton
        }
    }

    "Container with services that have missing dependencies" {

        val blueprint = kontainer {
            singleton(CounterService::class)
            singleton(InjectingService::class)
        }

        assertSoftly {
            val error = shouldThrow<KontainerInconsistent> {
                blueprint.create()
            }

            error.message shouldContain
                    "Parameter 'another' misses a dependency to '${AnotherSimpleService::class.qualifiedName}'"
        }
    }

    "Container with a dependency that is not know how to handle" {

        val blueprint = kontainer {
            singleton(InjectingSomethingWeird::class)
        }

        assertSoftly {
            val error = shouldThrow<KontainerInconsistent> {
                blueprint.create()
            }

            error.message shouldContain "Parameter 'weird' misses a dependency to 'kotlin.collections.Map'"
        }
    }

    "Container with singletons that injects other singletons" {

        val subject = kontainer {
            singleton(CounterService::class)
            singleton(AnotherSimpleService::class)
            singleton(InjectingService::class)
            singleton(AnotherInjectingService::class)
            singleton(DeeperInjectingService::class)
        }.create()

        assertSoftly {
            subject.get<CounterService>()::class shouldBe CounterService::class
            subject.getProvider<CounterService>().type shouldBe ServiceProvider.Type.Singleton

            subject.get<AnotherSimpleService>()::class shouldBe AnotherSimpleService::class
            subject.getProvider<AnotherSimpleService>().type shouldBe ServiceProvider.Type.Singleton

            subject.get<InjectingService>()::class shouldBe InjectingService::class
            subject.getProvider<InjectingService>().type shouldBe ServiceProvider.Type.Singleton

            subject.get<AnotherInjectingService>()::class shouldBe AnotherInjectingService::class
            subject.getProvider<AnotherInjectingService>().type shouldBe ServiceProvider.Type.Singleton

            subject.get<DeeperInjectingService>()::class shouldBe DeeperInjectingService::class
            subject.getProvider<DeeperInjectingService>().type shouldBe ServiceProvider.Type.Singleton
        }
    }

    "Global singletons must be shared across kontainer instances created by the same blueprint" {

        val blueprint = kontainer {
            singleton(CounterService::class)
            singleton(AnotherSimpleService::class)
            singleton(InjectingService::class)
        }

        val first = blueprint.create()
        first.get<CounterService>().inc()

        val second = blueprint.create()
        second.get<CounterService>().inc()
        second.get<CounterService>().inc()

        assertSoftly {
            @Suppress("RemoveExplicitTypeArguments")
            first.get<CounterService>() shouldBe second.get<CounterService>()
            first.get<CounterService>().get() shouldBe 3
            second.get<CounterService>().get() shouldBe 3

            @Suppress("RemoveExplicitTypeArguments")
            first.get<AnotherSimpleService>() shouldBe second.get<AnotherSimpleService>()

            @Suppress("RemoveExplicitTypeArguments")
            first.get<InjectingService>() shouldBe second.get<InjectingService>()
        }
    }

    "Getting a dynamic service multiple time must return the same instance" {

        val subject = kontainer {
            dynamic(CounterService::class, CounterServiceEx02::class)
        }.create()

        assertSoftly {
            subject.get(CounterService::class) shouldBeSameInstanceAs subject.get(CounterService::class)
        }
    }

    "Cloning a kontainer with singletons and dynamic services" {

        val blueprint = kontainer {
            singleton(CounterServiceEx01::class)
            dynamic(CounterServiceEx02::class)
        }

        // Get the original kontainer and initialize the dynamic counter with 10
        // NOTICE that we use [create] with callback here.
        //        Only this makes it possible that the cloned Kontainer gets a new instance of [CounterServiceEx02]
        val original = blueprint.create {
            with { CounterServiceEx02().apply { set(10) } }
        }

        // Inc singleton counter
        original.get(CounterServiceEx01::class).inc()
        // Inc dynamic counter
        original.get(CounterServiceEx02::class).inc()

        // Get a clone of the kontainer
        val clone = original.clone()
        // Inc singleton counter
        clone.get(CounterServiceEx01::class).inc()
        // Inc dynamic counter
        clone.get(CounterServiceEx02::class).inc()

        assertSoftly {
            original.get(CounterServiceEx01::class).let {
                it shouldBeSameInstanceAs clone.get(CounterServiceEx01::class)
                it.get() shouldBe 2
            }

            original.get(CounterServiceEx02::class).let {
                it shouldNotBeSameInstanceAs clone.get(CounterServiceEx02::class)
                it.get() shouldBe 11
            }

            clone.get(CounterServiceEx01::class).let {
                it shouldBeSameInstanceAs original.get(CounterServiceEx01::class)
                it.get() shouldBe 2
            }

            clone.get(CounterServiceEx02::class).let {
                it shouldNotBeSameInstanceAs original.get(CounterServiceEx02::class)
                it.get() shouldBe 11
            }
        }
    }

    "The 'Kontainer' itself is always available" {

        class MyService(val kontainer: Kontainer)

        val blueprint = kontainer {
            singleton(MyService::class)
        }

        val subjectOne = blueprint.create()
        val subjectTwo = blueprint.create()

        assertSoftly {

            subjectOne.get(Kontainer::class) shouldBeSameInstanceAs subjectOne
            subjectOne.getProvider(Kontainer::class).type shouldBe ServiceProvider.Type.Dynamic
            subjectOne.get(MyService::class).kontainer shouldBeSameInstanceAs subjectOne

            subjectTwo.get(Kontainer::class) shouldBeSameInstanceAs subjectTwo
            subjectTwo.getProvider(Kontainer::class).type shouldBe ServiceProvider.Type.Dynamic
            subjectTwo.get(MyService::class).kontainer shouldBeSameInstanceAs subjectTwo

            subjectOne.get(Kontainer::class) shouldNotBeSameInstanceAs subjectTwo.get(Kontainer::class)
        }
    }

    "The 'KontainerBlueprint' is always available" {

        class MyService(val blueprint: KontainerBlueprint)

        val blueprint = kontainer {
            singleton(MyService::class)
        }

        val subjectOne = blueprint.create()
        val subjectTwo = blueprint.create()

        assertSoftly {

            subjectOne.get(KontainerBlueprint::class) shouldBeSameInstanceAs blueprint
            subjectOne.getProvider(KontainerBlueprint::class).type shouldBe ServiceProvider.Type.Singleton
            subjectOne.get(MyService::class).blueprint shouldBeSameInstanceAs blueprint

            subjectTwo.get(KontainerBlueprint::class) shouldBeSameInstanceAs blueprint
            subjectTwo.getProvider(KontainerBlueprint::class).type shouldBe ServiceProvider.Type.Singleton
            subjectTwo.get(MyService::class).blueprint shouldBeSameInstanceAs blueprint
        }
    }
})
