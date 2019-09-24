package de.peekandpoke.ultra.kontainer.e2e

import de.peekandpoke.ultra.kontainer.*
import io.kotlintest.assertSoftly
import io.kotlintest.matchers.string.shouldContain
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import io.kotlintest.specs.StringSpec

class SingletonFactoriesSpec : StringSpec({

    "Singleton factory that has missing dependencies" {

        val blueprint = kontainer {
            singleton<SimpleService>()
            singleton(InjectingService::class) { simple: SimpleService, another: AnotherSimpleService ->
                InjectingService(simple, another)
            }
        }

        assertSoftly {
            val error = shouldThrow<KontainerInconsistent> {
                blueprint.useWith()
            }

            error.message shouldContain
                    "Parameter 'another' misses a dependency to '${AnotherSimpleService::class.qualifiedName}'"

            error.message shouldContain
                    "defined at"
        }
    }

    "Singleton factory with two dependencies" {

        val subject = kontainer {
            singleton<SimpleService>()
            singleton<AnotherSimpleService>()

            singleton(InjectingService::class) { simple: SimpleService, another: AnotherSimpleService ->
                InjectingService(simple, another)
            }
        }.useWith()

        assertSoftly {
            subject.get<InjectingService>()::class shouldBe InjectingService::class
        }
    }

    "Singleton factory with one config param" {

        val subject = kontainer {
            config("c1", 1)
            singleton { c1: Int -> ConfigIntInjecting(c1) }
        }.useWith()

        assertSoftly {

            subject.get<ConfigIntInjecting>()::class shouldBe ConfigIntInjecting::class
            subject.getProvider<ConfigIntInjecting>().type shouldBe ServiceProvider.Type.GlobalSingleton

            subject.get<ConfigIntInjecting>().configInt shouldBe 1
        }
    }

    "Singleton factory with two config params" {

        val subject = kontainer {
            config("c1", 1)
            config("c2", 10)
            singleton { c1: Int, c2: Int -> ConfigIntInjecting(c1 + c2) }
        }.useWith()

        subject.get<ConfigIntInjecting>().configInt shouldBe 11
    }

    "Singleton factory with three config params" {

        val subject = kontainer {
            config("c1", 1)
            config("c2", 10)
            config("c3", 100)
            singleton { c1: Int, c2: Int, c3: Int -> ConfigIntInjecting(c1 + c2 + c3) }
        }.useWith()

        subject.get<ConfigIntInjecting>().configInt shouldBe 111
    }

    "Singleton factory with four config params" {

        val subject = kontainer {
            config("c1", 1)
            config("c2", 10)
            config("c3", 100)
            config("c4", 1000)
            singleton { c1: Int, c2: Int, c3: Int, c4: Int -> ConfigIntInjecting(c1 + c2 + c3 + c4) }
        }.useWith()

        subject.get<ConfigIntInjecting>().configInt shouldBe 1111
    }

    "Singleton factory with five config params" {

        val subject = kontainer {
            config("c1", 1)
            config("c2", 10)
            config("c3", 100)
            config("c4", 1000)
            config("c5", 10000)
            singleton { c1: Int, c2: Int, c3: Int, c4: Int, c5: Int ->
                ConfigIntInjecting(c1 + c2 + c3 + c4 + c5)
            }
        }.useWith()

        subject.get<ConfigIntInjecting>().configInt shouldBe 11111
    }

    "Singleton factory with six config params" {

        val subject = kontainer {
            config("c1", 1)
            config("c2", 10)
            config("c3", 100)
            config("c4", 1000)
            config("c5", 10000)
            config("c6", 100000)
            singleton { c1: Int, c2: Int, c3: Int, c4: Int, c5: Int, c6: Int ->
                ConfigIntInjecting(c1 + c2 + c3 + c4 + c5 + c6)
            }
        }.useWith()

        subject.get<ConfigIntInjecting>().configInt shouldBe 111111
    }

    "Singleton factory with seven config params" {

        val subject = kontainer {
            config("c1", 1)
            config("c2", 10)
            config("c3", 100)
            config("c4", 1000)
            config("c5", 10000)
            config("c6", 100000)
            config("c7", 1000000)
            singleton { c1: Int, c2: Int, c3: Int, c4: Int, c5: Int, c6: Int, c7: Int ->
                ConfigIntInjecting(c1 + c2 + c3 + c4 + c5 + c6 + c7)
            }
        }.useWith()

        subject.get<ConfigIntInjecting>().configInt shouldBe 1111111
    }
})
