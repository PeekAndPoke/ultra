package de.peekandpoke.ultra.kontainer.e2e

import de.peekandpoke.ultra.kontainer.*
import io.kotlintest.assertSoftly
import io.kotlintest.matchers.string.shouldContain
import io.kotlintest.matchers.types.shouldBeSameInstanceAs
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import io.kotlintest.specs.StringSpec

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
    }

    fun validateWithoutBase(subject: Kontainer, value: Int) {

        assertSoftly {
            subject.get<Config>() shouldBeSameInstanceAs subject.get<Config>()

            subject.get<Base>()::class shouldBe Config::class
            subject.get<Config>()::class shouldBe Config::class
            subject.getProvider<Config>().type shouldBe ServiceProvider.Type.DynamicDefault

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
            subject.getProvider<Base>().type shouldBe ServiceProvider.Type.DynamicDefault

            subject.get<Base>().value shouldBe value
        }
    }

    "Dynamic factory that has missing dependencies" {

        val blueprint = kontainer {
            singleton<SimpleService>()

            dynamic(InjectingService::class) { simple: SimpleService, another: AnotherSimpleService ->
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

    "Dynamic factory with two dependencies" {

        val subject = kontainer {
            singleton<SimpleService>()
            singleton<AnotherSimpleService>()

            dynamic(InjectingService::class) { simple: SimpleService, another: AnotherSimpleService ->
                InjectingService(simple, another)
            }
        }.useWith()

        assertSoftly {
            subject.get<InjectingService>()::class shouldBe InjectingService::class
        }
    }

    "Dynamic factory with zero params" {

        val subject = kontainer {
            dynamic0 { Config(0) }
        }.useWith()

        validateWithoutBase(subject, 0)
    }

    "Dynamic factory with zero params defined with bas class" {

        val subject = kontainer {
            dynamic0(Base::class) { Config(0) }
        }.useWith()

        validateWithBase(subject, 0)
    }

    "Dynamic factory with one param" {

        val subject = kontainer {
            module(configs)

            dynamic { c1: Int -> Config(c1) }
        }.useWith()

        validateWithoutBase(subject, 1)
    }

    "Dynamic factory with one param defined with base class" {

        val subject = kontainer {
            module(configs)

            dynamic(Base::class) { c1: Int -> Config(c1) }
        }.useWith()

        validateWithBase(subject, 1)
    }

    "Dynamic factory with two config params" {

        val subject = kontainer {
            module(configs)

            dynamic { c1: Int, c2: Int -> Config(c1 + c2) }
        }.useWith()

        validateWithoutBase(subject, 11)
    }

    "Dynamic factory with two config params defined with base class" {

        val subject = kontainer {
            module(configs)

            dynamic(Base::class) { c1: Int, c2: Int -> Config(c1 + c2) }
        }.useWith()

        validateWithBase(subject, 11)
    }

    "Dynamic factory with three config params" {

        val subject = kontainer {
            module(configs)

            dynamic { c1: Int, c2: Int, c3: Int -> Config(c1 + c2 + c3) }
        }.useWith()

        validateWithoutBase(subject, 111)
    }

    "Dynamic factory with three config params defined with base class" {

        val subject = kontainer {
            module(configs)

            dynamic(Base::class) { c1: Int, c2: Int, c3: Int -> Config(c1 + c2 + c3) }
        }.useWith()

        validateWithBase(subject, 111)
    }

    "Dynamic factory with four config params" {

        val subject = kontainer {
            module(configs)

            dynamic { c1: Int, c2: Int, c3: Int, c4: Int -> Config(c1 + c2 + c3 + c4) }
        }.useWith()

        validateWithoutBase(subject, 1111)
    }

    "Dynamic factory with four config params defined with base class" {

        val subject = kontainer {
            module(configs)

            dynamic(Base::class) { c1: Int, c2: Int, c3: Int, c4: Int -> Config(c1 + c2 + c3 + c4) }
        }.useWith()

        validateWithBase(subject, 1111)
    }

    "Dynamic factory with five config params" {

        val subject = kontainer {
            module(configs)

            dynamic { c1: Int, c2: Int, c3: Int, c4: Int, c5: Int -> Config(c1 + c2 + c3 + c4 + c5) }
        }.useWith()

        validateWithoutBase(subject, 11111)
    }

    "Dynamic factory with five config params defined with base class" {

        val subject = kontainer {
            module(configs)

            dynamic(Base::class) { c1: Int, c2: Int, c3: Int, c4: Int, c5: Int ->
                Config(c1 + c2 + c3 + c4 + c5)
            }
        }.useWith()

        validateWithBase(subject, 11111)
    }

    "Dynamic factory with six config params" {

        val subject = kontainer {
            module(configs)

            dynamic { c1: Int, c2: Int, c3: Int, c4: Int, c5: Int, c6: Int ->
                Config(c1 + c2 + c3 + c4 + c5 + c6)
            }
        }.useWith()

        validateWithoutBase(subject, 111111)
    }

    "Dynamic factory with six config params defined with base class" {

        val subject = kontainer {
            module(configs)

            dynamic(Base::class) { c1: Int, c2: Int, c3: Int, c4: Int, c5: Int, c6: Int ->
                Config(c1 + c2 + c3 + c4 + c5 + c6)
            }
        }.useWith()

        validateWithBase(subject, 111111)
    }

    "Dynamic factory with seven config params" {

        val subject = kontainer {
            module(configs)

            dynamic { c1: Int, c2: Int, c3: Int, c4: Int, c5: Int, c6: Int, c7: Int ->
                Config(c1 + c2 + c3 + c4 + c5 + c6 + c7)
            }
        }.useWith()

        validateWithoutBase(subject, 1111111)
    }

    "Dynamic factory with seven config params defined with base class" {

        val subject = kontainer {
            module(configs)

            dynamic(Base::class) { c1: Int, c2: Int, c3: Int, c4: Int, c5: Int, c6: Int, c7: Int ->
                Config(c1 + c2 + c3 + c4 + c5 + c6 + c7)
            }
        }.useWith()

        validateWithBase(subject, 1111111)
    }
})
