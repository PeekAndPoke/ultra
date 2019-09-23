package de.peekandpoke.ultra.kontainer.e2e

import de.peekandpoke.ultra.kontainer.KontainerInconsistent
import de.peekandpoke.ultra.kontainer.ServiceNotFound
import de.peekandpoke.ultra.kontainer.ServiceProvider
import de.peekandpoke.ultra.kontainer.kontainer
import io.kotlintest.assertSoftly
import io.kotlintest.matchers.string.shouldContain
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

    "Creating a container with a singleton service" {

        val subject = kontainer {
            singleton<SimpleService>()
        }.useWith()

        assertSoftly {

            subject.get<SimpleService>()::class shouldBe SimpleService::class
            subject.getProvider<SimpleService>().type shouldBe ServiceProvider.Type.GlobalSingleton
        }

    }

    "Creating a container with multiple singleton services" {

        val subject = kontainer {
            singleton<SimpleService>()
            singleton<AnotherSimpleService>()
        }.useWith()

        assertSoftly {
            subject.get<SimpleService>()::class shouldBe SimpleService::class
            subject.getProvider<SimpleService>().type shouldBe ServiceProvider.Type.GlobalSingleton

            subject.get<AnotherSimpleService>()::class shouldBe AnotherSimpleService::class
            subject.getProvider<AnotherSimpleService>().type shouldBe ServiceProvider.Type.GlobalSingleton
        }
    }

    "Creating a container with missing dependencies" {

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

    "Creating a container with a dependency that is not know how to handle" {

        val blueprint = kontainer {
            singleton<InjectingSomethingWeird>()
        }

        assertSoftly {
            val error = shouldThrow<KontainerInconsistent> {
                blueprint.useWith()
            }

            error.message shouldContain "Parameter 'weird' has no known way to inject a"
        }
    }

    "Creating a container with singletons that inject other singletons" {

        val subject = kontainer {
            singleton<SimpleService>()
            singleton<AnotherSimpleService>()
            singleton<InjectingService>()
            singleton<AnotherInjectingService>()
            singleton<DeeperInjectingService>()
        }.useWith()

        assertSoftly {
            subject.get<SimpleService>()::class shouldBe SimpleService::class
            subject.getProvider<SimpleService>().type shouldBe ServiceProvider.Type.GlobalSingleton

            subject.get<AnotherSimpleService>()::class shouldBe AnotherSimpleService::class
            subject.getProvider<AnotherSimpleService>().type shouldBe ServiceProvider.Type.GlobalSingleton

            subject.get<InjectingService>()::class shouldBe InjectingService::class
            subject.getProvider<InjectingService>().type shouldBe ServiceProvider.Type.GlobalSingleton

            subject.get<AnotherInjectingService>()::class shouldBe AnotherInjectingService::class
            subject.getProvider<AnotherInjectingService>().type shouldBe ServiceProvider.Type.GlobalSingleton

            subject.get<DeeperInjectingService>()::class shouldBe DeeperInjectingService::class
            subject.getProvider<DeeperInjectingService>().type shouldBe ServiceProvider.Type.GlobalSingleton
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
})
