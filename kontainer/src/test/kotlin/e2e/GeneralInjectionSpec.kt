package de.peekandpoke.ultra.kontainer.e2e

import de.peekandpoke.ultra.kontainer.*
import io.kotlintest.assertSoftly
import io.kotlintest.matchers.string.shouldContain
import io.kotlintest.matchers.types.shouldBeSameInstanceAs
import io.kotlintest.matchers.types.shouldNotBeSameInstanceAs
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
import io.kotlintest.shouldThrow
import io.kotlintest.specs.StringSpec


class GeneralInjectionSpec : StringSpec({

    "Creating an empty container" {

        val blueprint = kontainer {}

        val subject = blueprint.useWith()

        subject shouldNotBe null
    }

    "Requesting a singleton that is not registered" {

        val result = kontainer { }.useWith()

        shouldThrow<ServiceNotFound> {
            result.get<SimpleService>()
        }
    }

    "Container with a singleton service" {

        val subject = kontainer {
            singleton<SimpleService>()
        }.useWith()

        assertSoftly {

            subject.get<SimpleService>()::class shouldBe SimpleService::class
            subject.getProvider<SimpleService>().type shouldBe ServiceProvider.Type.Singleton
        }
    }

    "Container with a singleton service (::class style)" {

        val subject = kontainer {
            singleton(SimpleService::class)
        }.useWith()

        assertSoftly {

            subject.get<SimpleService>()::class shouldBe SimpleService::class
            subject.getProvider<SimpleService>().type shouldBe ServiceProvider.Type.Singleton
        }
    }

    "Container with a singleton service from existing instance" {

        val subject = kontainer {
            instance(SimpleService())
        }.useWith()

        assertSoftly {

            subject.get<SimpleService>() shouldBeSameInstanceAs subject.get(SimpleService::class)

            subject.get<SimpleService>()::class shouldBe SimpleService::class
            subject.getProvider<SimpleService>().type shouldBe ServiceProvider.Type.Singleton
        }
    }

    "Container with a singleton service from existing instance registered with base class" {

        val subject = kontainer {
            instance(SimpleService::class, SuperSimpleService())
        }.useWith()

        assertSoftly {

            shouldThrow<ServiceNotFound> {
                subject.get<SuperSimpleService>()
            }

            subject.get<SimpleService>() shouldBeSameInstanceAs subject.get(SimpleService::class)

            subject.get<SimpleService>()::class shouldBe SuperSimpleService::class
            subject.getProvider<SimpleService>().type shouldBe ServiceProvider.Type.Singleton
        }
    }

    "Container with multiple singleton services" {

        val subject = kontainer {
            singleton<SimpleService>()
            singleton<AnotherSimpleService>()
        }.useWith()

        assertSoftly {
            subject.get<SimpleService>()::class shouldBe SimpleService::class
            subject.getProvider<SimpleService>().type shouldBe ServiceProvider.Type.Singleton

            subject.get<AnotherSimpleService>()::class shouldBe AnotherSimpleService::class
            subject.getProvider<AnotherSimpleService>().type shouldBe ServiceProvider.Type.Singleton
        }
    }

    "Container with services that have missing dependencies" {

        val blueprint = kontainer {
            singleton<SimpleService>()
            singleton<InjectingService>()
        }

        assertSoftly {
            val error = shouldThrow<KontainerInconsistent> {
                blueprint.useWith()
            }

            error.message shouldContain
                    "Parameter 'another' misses a dependency to '${AnotherSimpleService::class.qualifiedName}'"
        }
    }

    "Container with a dependency that is not know how to handle" {

        val blueprint = kontainer {
            singleton<InjectingSomethingWeird>()
        }

        assertSoftly {
            val error = shouldThrow<KontainerInconsistent> {
                blueprint.useWith()
            }

            error.message shouldContain "Parameter 'weird' misses a dependency to 'kotlin.collections.Map'"
        }
    }

    "Container with singletons that injects other singletons" {

        val subject = kontainer {
            singleton<SimpleService>()
            singleton<AnotherSimpleService>()
            singleton<InjectingService>()
            singleton<AnotherInjectingService>()
            singleton<DeeperInjectingService>()
        }.useWith()

        assertSoftly {
            subject.get<SimpleService>()::class shouldBe SimpleService::class
            subject.getProvider<SimpleService>().type shouldBe ServiceProvider.Type.Singleton

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

    "Reusing a blueprint with global singletons" {

        val blueprint = kontainer {
            singleton<SimpleService>()
            singleton<AnotherSimpleService>()
            singleton<InjectingService>()
        }

        val first = blueprint.useWith()
        first.get<SimpleService>().inc()

        val second = blueprint.useWith()
        second.get<SimpleService>().inc()
        second.get<SimpleService>().inc()

        assertSoftly {
            @Suppress("RemoveExplicitTypeArguments")
            first.get<SimpleService>() shouldBe second.get<SimpleService>()
            first.get<SimpleService>().get() shouldBe 3
            second.get<SimpleService>().get() shouldBe 3

            @Suppress("RemoveExplicitTypeArguments")
            first.get<AnotherSimpleService>() shouldBe second.get<AnotherSimpleService>()

            @Suppress("RemoveExplicitTypeArguments")
            first.get<InjectingService>() shouldBe second.get<InjectingService>()
        }
    }

    "The 'Kontainer' itself is always available" {

        class MyService(val kontainer: Kontainer)

        val blueprint = kontainer {
            singleton(MyService::class)
        }

        val subjectOne = blueprint.useWith()
        val subjectTwo = blueprint.useWith()

        assertSoftly {

            subjectOne.get(Kontainer::class) shouldBeSameInstanceAs subjectOne
            subjectOne.getProvider(Kontainer::class).type shouldBe ServiceProvider.Type.DynamicDefault
            subjectOne.get(MyService::class).kontainer shouldBeSameInstanceAs subjectOne

            subjectTwo.get(Kontainer::class) shouldBeSameInstanceAs subjectTwo
            subjectTwo.getProvider(Kontainer::class).type shouldBe ServiceProvider.Type.DynamicDefault
            subjectTwo.get(MyService::class).kontainer shouldBeSameInstanceAs subjectTwo

            subjectOne.get(Kontainer::class) shouldNotBeSameInstanceAs subjectTwo.get(Kontainer::class)
        }
    }

    "The 'KontainerBlueprint' is always available" {

        class MyService(val blueprint: KontainerBlueprint)

        val blueprint = kontainer {
            singleton(MyService::class)
        }

        val subjectOne = blueprint.useWith()
        val subjectTwo = blueprint.useWith()

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
