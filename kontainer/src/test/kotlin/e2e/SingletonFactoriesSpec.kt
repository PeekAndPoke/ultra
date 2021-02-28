package de.peekandpoke.ultra.kontainer.e2e

import de.peekandpoke.ultra.kontainer.AnotherSimpleService
import de.peekandpoke.ultra.kontainer.InjectingService
import de.peekandpoke.ultra.kontainer.Kontainer
import de.peekandpoke.ultra.kontainer.KontainerInconsistent
import de.peekandpoke.ultra.kontainer.ServiceNotFound
import de.peekandpoke.ultra.kontainer.ServiceProvider
import de.peekandpoke.ultra.kontainer.SimpleService
import de.peekandpoke.ultra.kontainer.kontainer
import de.peekandpoke.ultra.kontainer.module
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeSameInstanceAs

class SingletonFactoriesSpec : StringSpec({

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
            subject.getProvider<Config>().type shouldBe ServiceProvider.Type.Singleton

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
            subject.getProvider<Base>().type shouldBe ServiceProvider.Type.Singleton

            subject.get<Base>().value shouldBe value
        }
    }

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

        val blueprint = kontainer {
            singleton<SimpleService>()
            singleton<AnotherSimpleService>()

            singleton(InjectingService::class) { simple: SimpleService, another: AnotherSimpleService ->
                InjectingService(simple, another)
            }
        }

        val subject = blueprint.useWith()

        assertSoftly {
            subject.get<InjectingService>()::class shouldBe InjectingService::class
        }
    }

    "Singleton factory with zero params" {

        val blueprint = kontainer {
            singleton { Config(0) }
        }

        val subject = blueprint.useWith()

        validateWithoutBase(subject, 0)
    }

    "Singleton factory with zero params defined with bas class" {

        val blueprint = kontainer {
            singleton(Base::class) { Config(0) }
        }

        val subject = blueprint.useWith()

        validateWithBase(subject, 0)
    }

    "Singleton factory with one param" {

        val blueprint = kontainer {
            module(configs)

            singleton { c1: Int -> Config(c1) }
        }

        val subject = blueprint.useWith()

        validateWithoutBase(subject, 1)
    }

    "Singleton factory with one param defined with base class" {

        val blueprint = kontainer {
            module(configs)

            singleton(Base::class) { c1: Int -> Config(c1) }
        }

        val subject = blueprint.useWith()

        validateWithBase(subject, 1)
    }

    "Singleton factory with two config params" {

        val blueprint = kontainer {
            module(configs)

            singleton { c1: Int, c2: Int -> Config(c1 + c2) }
        }

        val subject = blueprint.useWith()

        validateWithoutBase(subject, 11)
    }

    "Singleton factory with two config params defined with base class" {

        val blueprint = kontainer {
            module(configs)

            singleton(Base::class) { c1: Int, c2: Int -> Config(c1 + c2) }
        }

        val subject = blueprint.useWith()

        validateWithBase(subject, 11)
    }

    "Singleton factory with three config params" {

        val blueprint = kontainer {
            module(configs)

            singleton { c1: Int, c2: Int, c3: Int -> Config(c1 + c2 + c3) }
        }

        val subject = blueprint.useWith()

        validateWithoutBase(subject, 111)
    }

    "Singleton factory with three config params defined with base class" {

        val blueprint = kontainer {
            module(configs)

            singleton(Base::class) { c1: Int, c2: Int, c3: Int -> Config(c1 + c2 + c3) }
        }

        val subject = blueprint.useWith()

        validateWithBase(subject, 111)
    }

    "Singleton factory with four config params" {

        val blueprint = kontainer {
            module(configs)

            singleton { c1: Int, c2: Int, c3: Int, c4: Int -> Config(c1 + c2 + c3 + c4) }
        }

        val subject = blueprint.useWith()

        validateWithoutBase(subject, 1111)
    }

    "Singleton factory with four config params defined with base class" {

        val blueprint = kontainer {
            module(configs)

            singleton(Base::class) { c1: Int, c2: Int, c3: Int, c4: Int -> Config(c1 + c2 + c3 + c4) }
        }

        val subject = blueprint.useWith()

        validateWithBase(subject, 1111)
    }

    "Singleton factory with five config params" {

        val blueprint = kontainer {
            module(configs)

            singleton { c1: Int, c2: Int, c3: Int, c4: Int, c5: Int -> Config(c1 + c2 + c3 + c4 + c5) }
        }

        val subject = blueprint.useWith()

        validateWithoutBase(subject, 11111)
    }

    "Singleton factory with five config params defined with base class" {

        val blueprint = kontainer {
            module(configs)

            singleton(Base::class) { c1: Int, c2: Int, c3: Int, c4: Int, c5: Int ->
                Config(c1 + c2 + c3 + c4 + c5)
            }
        }

        val subject = blueprint.useWith()

        validateWithBase(subject, 11111)
    }

    "Singleton factory with six config params" {

        val blueprint = kontainer {
            module(configs)

            singleton { c1: Int, c2: Int, c3: Int, c4: Int, c5: Int, c6: Int ->
                Config(c1 + c2 + c3 + c4 + c5 + c6)
            }
        }

        val subject = blueprint.useWith()

        validateWithoutBase(subject, 111111)
    }

    "Singleton factory with six config params defined with base class" {

        val blueprint = kontainer {
            module(configs)

            singleton(Base::class) { c1: Int, c2: Int, c3: Int, c4: Int, c5: Int, c6: Int ->
                Config(c1 + c2 + c3 + c4 + c5 + c6)
            }
        }

        val subject = blueprint.useWith()

        validateWithBase(subject, 111111)
    }

    "Singleton factory with seven config params" {

        val blueprint = kontainer {
            module(configs)

            singleton { c1: Int, c2: Int, c3: Int, c4: Int, c5: Int, c6: Int, c7: Int ->
                Config(c1 + c2 + c3 + c4 + c5 + c6 + c7)
            }
        }

        val subject = blueprint.useWith()

        validateWithoutBase(subject, 1111111)
    }

    "Singleton factory with seven config params defined with base class" {

        val blueprint = kontainer {
            module(configs)

            singleton(Base::class) { c1: Int, c2: Int, c3: Int, c4: Int, c5: Int, c6: Int, c7: Int ->
                Config(c1 + c2 + c3 + c4 + c5 + c6 + c7)
            }
        }

        val subject = blueprint.useWith()

        validateWithBase(subject, 1111111)
    }

    "Singleton factory with eight config params" {

        val blueprint = kontainer {
            module(configs)

            singleton { c1: Int, c2: Int, c3: Int, c4: Int, c5: Int, c6: Int, c7: Int, c8: Int ->
                Config(c1 + c2 + c3 + c4 + c5 + c6 + c7 + c8)
            }
        }

        val subject = blueprint.useWith()

        validateWithoutBase(subject, 11111111)
    }

    "Singleton factory with eight config params defined with base class" {

        val blueprint = kontainer {
            module(configs)

            singleton(Base::class) { c1: Int, c2: Int, c3: Int, c4: Int, c5: Int, c6: Int, c7: Int, c8: Int ->
                Config(c1 + c2 + c3 + c4 + c5 + c6 + c7 + c8)
            }
        }

        val subject = blueprint.useWith()

        validateWithBase(subject, 11111111)
    }

    "Singleton factory with nine config params" {

        val blueprint = kontainer {
            module(configs)

            singleton { c1: Int, c2: Int, c3: Int, c4: Int, c5: Int, c6: Int, c7: Int, c8: Int, c9: Int ->
                Config(c1 + c2 + c3 + c4 + c5 + c6 + c7 + c8 + c9)
            }
        }

        val subject = blueprint.useWith()

        validateWithoutBase(subject, 111111111)
    }

    "Singleton factory with nine config params defined with base class" {

        val blueprint = kontainer {
            module(configs)

            singleton(Base::class) { c1: Int, c2: Int, c3: Int, c4: Int, c5: Int, c6: Int, c7: Int, c8: Int, c9: Int ->
                Config(c1 + c2 + c3 + c4 + c5 + c6 + c7 + c8 + c9)
            }
        }

        val subject = blueprint.useWith()

        validateWithBase(subject, 111111111)
    }

    "Singleton factory with ten config params" {

        val blueprint = kontainer {
            module(configs)

            singleton { c1: Int, c2: Int, c3: Int, c4: Int, c5: Int, c6: Int, c7: Int, c8: Int, c9: Int, c10: Int ->
                Config(c1 + c2 + c3 + c4 + c5 + c6 + c7 + c8 + c9 + c10)
            }
        }

        val subject = blueprint.useWith()

        validateWithoutBase(subject, 1111111111)
    }

    "Singleton factory with ten config params defined with base class" {

        val blueprint = kontainer {
            module(configs)

            singleton(Base::class) { c1: Int, c2: Int, c3: Int, c4: Int, c5: Int, c6: Int, c7: Int, c8: Int, c9: Int, c10: Int ->
                Config(c1 + c2 + c3 + c4 + c5 + c6 + c7 + c8 + c9 + c10)
            }
        }

        val subject = blueprint.useWith()

        validateWithBase(subject, 1111111111)
    }

    "Singleton factory with zero config params defined via dynamic function" {

        val blueprint = kontainer {
            module(configs)

            val provider: Function<Config> = { Config(0) }

            singleton(Base::class, provider)

        }

        val subject = blueprint.useWith()

        validateWithBase(subject, 0)
    }

    "Singleton factory with one config params defined via dynamic function" {

        val blueprint = kontainer {
            module(configs)

            val provider: Function<Config> = { c1: Int -> Config(c1) }

            singleton(Base::class, provider)
        }

        val subject = blueprint.useWith()

        validateWithBase(subject, 1)
    }

    "Singleton factory with seven config params defined via dynamic function" {

        val blueprint = kontainer {
            module(configs)

            val provider: Function<Config> = { c1: Int, c2: Int, c3: Int, c4: Int, c5: Int, c6: Int, c7: Int ->
                Config(c1 + c2 + c3 + c4 + c5 + c6 + c7)
            }

            singleton(Base::class, provider)
        }

        val subject = blueprint.useWith()

        validateWithBase(subject, 1111111)
    }

    "Singleton factory with ten config params defined via dynamic function" {

        val blueprint = kontainer {
            module(configs)

            val provider: Function<Config> =
                { c1: Int, c2: Int, c3: Int, c4: Int, c5: Int, c6: Int, c7: Int, c8: Int, c9: Int, c10: Int ->
                    Config(c1 + c2 + c3 + c4 + c5 + c6 + c7 + c8 + c9 + c10)
                }

            singleton(Base::class, provider)
        }

        val subject = blueprint.useWith()

        validateWithBase(subject, 1111111111)
    }
})
