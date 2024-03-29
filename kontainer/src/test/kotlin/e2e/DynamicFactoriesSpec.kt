package de.peekandpoke.ultra.kontainer.e2e

import de.peekandpoke.ultra.kontainer.AnotherSimpleService
import de.peekandpoke.ultra.kontainer.CounterService
import de.peekandpoke.ultra.kontainer.InjectingService
import de.peekandpoke.ultra.kontainer.Kontainer
import de.peekandpoke.ultra.kontainer.KontainerInconsistent
import de.peekandpoke.ultra.kontainer.ServiceNotFound
import de.peekandpoke.ultra.kontainer.ServiceProvider
import de.peekandpoke.ultra.kontainer.kontainer
import de.peekandpoke.ultra.kontainer.module
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeSameInstanceAs

class DynamicFactoriesSpec : StringSpec({

    abstract class Base {
        abstract val value: Int
    }

    data class Config(override val value: Int) : Base()

    val configs = module {
        config("c1", 1)
        config("c2", 10)
        config("c3", 100)
        config("c4", 1000)
        config("c5", 10000)
        config("c6", 100000)
        config("c7", 1000000)
        config("c8", 10000000)
        config("c9", 100000000)
        config("c10", 1000000000)
    }

    fun validateWithoutBase(subject: Kontainer, value: Int) {

        assertSoftly {
            subject.get<Config>() shouldBeSameInstanceAs subject.get<Config>()

            subject.get<Base>()::class shouldBe Config::class
            subject.get<Config>()::class shouldBe Config::class
            subject.getProvider<Config>().type shouldBe ServiceProvider.Type.Dynamic

            subject.get<Config>().value shouldBe value
        }
    }

    fun validateWithBase(subject: Kontainer, value: Int) {

        assertSoftly {
            shouldThrow<ServiceNotFound> {
                subject.get(Config::class)
            }

            subject.get<Base>() shouldBeSameInstanceAs subject.get<Base>()

            subject.get<Base>()::class shouldBe Config::class
            subject.getProvider<Base>().type shouldBe ServiceProvider.Type.Dynamic

            subject.get<Base>().value shouldBe value
        }
    }

    "Dynamic factory that has missing dependencies" {

        val blueprint = kontainer {
            singleton(CounterService::class)

            dynamic(InjectingService::class) { simple: CounterService, another: AnotherSimpleService ->
                InjectingService(simple, another)
            }
        }

        assertSoftly {
            val error = shouldThrow<KontainerInconsistent> {
                blueprint.create()
            }

            error.message shouldContain
                    "Parameter 'another' misses a dependency to '${AnotherSimpleService::class.qualifiedName}'"

            error.message shouldContain
                    "defined at"
        }
    }

    "Dynamic factory with two dependencies" {

        val subject = kontainer {
            singleton(CounterService::class)
            singleton(AnotherSimpleService::class)

            dynamic(InjectingService::class) { simple: CounterService, another: AnotherSimpleService ->
                InjectingService(simple, another)
            }
        }.create()

        assertSoftly {
            subject.get<InjectingService>()::class shouldBe InjectingService::class
        }
    }

    "Dynamic factory with zero params" {

        val subject = kontainer {
            dynamic0(Config::class) { Config(0) }
        }.create()

        validateWithoutBase(subject, 0)
    }

    "Dynamic factory with zero params defined with bas class" {

        val subject = kontainer {
            dynamic0(Base::class) { Config(0) }
        }.create()

        validateWithBase(subject, 0)
    }

    "Dynamic factory with one param" {

        val subject = kontainer {
            module(configs)

            dynamic(Config::class) { c1: Int -> Config(c1) }
        }.create()

        validateWithoutBase(subject, 1)
    }

    "Dynamic factory with one param defined with base class" {

        val subject = kontainer {
            module(configs)

            dynamic(Base::class) { c1: Int -> Config(c1) }
        }.create()

        validateWithBase(subject, 1)
    }

    "Dynamic factory with two config params" {

        val subject = kontainer {
            module(configs)

            dynamic(Config::class) { c1: Int, c2: Int -> Config(c1 + c2) }
        }.create()

        validateWithoutBase(subject, 11)
    }

    "Dynamic factory with two config params defined with base class" {

        val subject = kontainer {
            module(configs)

            dynamic(Base::class) { c1: Int, c2: Int -> Config(c1 + c2) }
        }.create()

        validateWithBase(subject, 11)
    }

    "Dynamic factory with three config params" {

        val subject = kontainer {
            module(configs)

            dynamic(Config::class) { c1: Int, c2: Int, c3: Int -> Config(c1 + c2 + c3) }
        }.create()

        validateWithoutBase(subject, 111)
    }

    "Dynamic factory with three config params defined with base class" {

        val subject = kontainer {
            module(configs)

            dynamic(Base::class) { c1: Int, c2: Int, c3: Int -> Config(c1 + c2 + c3) }
        }.create()

        validateWithBase(subject, 111)
    }

    "Dynamic factory with four config params" {

        val subject = kontainer {
            module(configs)

            dynamic(Config::class) { c1: Int, c2: Int, c3: Int, c4: Int -> Config(c1 + c2 + c3 + c4) }
        }.create()

        validateWithoutBase(subject, 1111)
    }

    "Dynamic factory with four config params defined with base class" {

        val subject = kontainer {
            module(configs)

            dynamic(Base::class) { c1: Int, c2: Int, c3: Int, c4: Int -> Config(c1 + c2 + c3 + c4) }
        }.create()

        validateWithBase(subject, 1111)
    }

    "Dynamic factory with five config params" {

        val subject = kontainer {
            module(configs)

            dynamic(Config::class) { c1: Int, c2: Int, c3: Int, c4: Int, c5: Int -> Config(c1 + c2 + c3 + c4 + c5) }
        }.create()

        validateWithoutBase(subject, 11111)
    }

    "Dynamic factory with five config params defined with base class" {

        val subject = kontainer {
            module(configs)

            dynamic(Base::class) { c1: Int, c2: Int, c3: Int, c4: Int, c5: Int ->
                Config(c1 + c2 + c3 + c4 + c5)
            }
        }.create()

        validateWithBase(subject, 11111)
    }

    "Dynamic factory with six config params" {

        val subject = kontainer {
            module(configs)

            dynamic(Config::class) { c1: Int, c2: Int, c3: Int, c4: Int, c5: Int, c6: Int ->
                Config(c1 + c2 + c3 + c4 + c5 + c6)
            }
        }.create()

        validateWithoutBase(subject, 111111)
    }

    "Dynamic factory with six config params defined with base class" {

        val subject = kontainer {
            module(configs)

            dynamic(Base::class) { c1: Int, c2: Int, c3: Int, c4: Int, c5: Int, c6: Int ->
                Config(c1 + c2 + c3 + c4 + c5 + c6)
            }
        }.create()

        validateWithBase(subject, 111111)
    }

    "Dynamic factory with seven config params" {

        val subject = kontainer {
            module(configs)

            dynamic(Config::class) { c1: Int, c2: Int, c3: Int, c4: Int, c5: Int, c6: Int, c7: Int ->
                Config(c1 + c2 + c3 + c4 + c5 + c6 + c7)
            }
        }.create()

        validateWithoutBase(subject, 1111111)
    }

    "Dynamic factory with seven config params defined with base class" {

        val subject = kontainer {
            module(configs)

            dynamic(Base::class) { c1: Int, c2: Int, c3: Int, c4: Int, c5: Int, c6: Int, c7: Int ->
                Config(c1 + c2 + c3 + c4 + c5 + c6 + c7)
            }
        }.create()

        validateWithBase(subject, 1111111)
    }

    "Dynamic factory with one config params defined via dynamic function" {

        val blueprint = kontainer {
            module(configs)

            val provider: Function<Config> = { c1: Int -> Config(c1) }

            dynamic(Base::class, provider)
        }

        val subject = blueprint.create()

        validateWithBase(subject, 1)
    }

    "Dynamic factory with seven config params defined via dynamic function" {

        val blueprint = kontainer {
            module(configs)

            val provider: Function<Config> = { c1: Int, c2: Int, c3: Int, c4: Int, c5: Int, c6: Int, c7: Int ->
                Config(c1 + c2 + c3 + c4 + c5 + c6 + c7)
            }

            dynamic(Base::class, provider)
        }

        val subject = blueprint.create()

        validateWithBase(subject, 1111111)
    }

    "Dynamic factory with ten config params defined via dynamic function" {

        val blueprint = kontainer {
            module(configs)

            val provider: Function<Config> =
                { c1: Int, c2: Int, c3: Int, c4: Int, c5: Int, c6: Int, c7: Int, c8: Int, c9: Int, c10: Int ->
                    Config(c1 + c2 + c3 + c4 + c5 + c6 + c7 + c8 + c9 + c10)
                }

            dynamic(Base::class, provider)
        }

        val subject = blueprint.create()

        validateWithBase(subject, 1111111111)
    }
})
