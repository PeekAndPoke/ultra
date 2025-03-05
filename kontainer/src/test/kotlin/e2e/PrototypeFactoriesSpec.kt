package de.peekandpoke.ultra.kontainer.e2e

import de.peekandpoke.ultra.kontainer.Kontainer
import de.peekandpoke.ultra.kontainer.KontainerInconsistent
import de.peekandpoke.ultra.kontainer.ServiceNotFound
import de.peekandpoke.ultra.kontainer.ServiceProvider
import de.peekandpoke.ultra.kontainer.getName
import de.peekandpoke.ultra.kontainer.kontainer
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldNotBeSameInstanceAs

class PrototypeFactoriesSpec : StringSpec({

    abstract class Base {
        abstract val value: Int
    }

    data class Config(override val value: Int) : Base()

    fun validateWithoutBase(subject: Kontainer, value: Int) {

        assertSoftly {
            subject.get<Config>() shouldNotBeSameInstanceAs subject.get<Config>()

            subject.get<Base>()::class shouldBe Config::class
            subject.get<Config>()::class shouldBe Config::class
            subject.getProvider<Config>().type shouldBe ServiceProvider.Type.Prototype

            subject.get<Config>().value shouldBe value
        }
    }

    fun validateWithBase(subject: Kontainer, value: Int) {

        assertSoftly {
            shouldThrow<ServiceNotFound> {
                subject.get(Config::class)
            }

            subject.get<Base>() shouldNotBeSameInstanceAs subject.get<Base>()

            subject.get<Base>()::class shouldBe Config::class
            subject.getProvider<Base>().type shouldBe ServiceProvider.Type.Prototype

            subject.get<Base>().value shouldBe value
        }
    }

    "Factory that has missing dependencies" {

        val blueprint = kontainer {
            singleton(CounterService::class)

            prototype(InjectingService::class) { simple: CounterService, another: AnotherSimpleService ->
                InjectingService(simple, another)
            }
        }

        assertSoftly {
            val error = shouldThrow<KontainerInconsistent> {
                blueprint.create()
            }

            error.message shouldContain
                    "Parameter 'p2' misses a dependency to ${AnotherSimpleService::class.getName()}"

            error.message shouldContain
                    "defined at"
        }
    }

    "Factory with two dependencies" {

        val subject = kontainer {
            singleton(CounterService::class)
            singleton(AnotherSimpleService::class)

            prototype(InjectingService::class) { simple: CounterService, another: AnotherSimpleService ->
                InjectingService(simple, another)
            }
        }.create()

        assertSoftly {
            subject.get<InjectingService>()::class shouldBe InjectingService::class
        }
    }

    "Factory with zero params" {

        val subject = kontainer {
            prototype(Config::class) { Config(0) }
        }.create()

        validateWithoutBase(subject, 0)
    }

    "Factory with zero params defined with base class" {

        val subject = kontainer {
            prototype(Base::class) { Config(0) }
        }.create()

        validateWithBase(subject, 0)
    }

    "Factory with one param" {

        val subject = kontainer {
            module(common)

            prototype(Config::class) { s1: S01 ->
                Config(s1.v)
            }
        }.create()

        validateWithoutBase(subject, 1)
    }

    "Factory with one param defined with base class" {

        val subject = kontainer {
            module(common)

            prototype(Base::class) { s1: S01 ->
                Config(s1.v)
            }
        }.create()

        validateWithBase(subject, 1)
    }

    "Factory with two params" {

        val subject = kontainer {
            module(common)

            prototype(Config::class) { s1: S01, s2: S02 ->
                Config(s1.v + s2.v)
            }
        }.create()

        validateWithoutBase(subject, 11)
    }

    "Factory with two params defined with base class" {

        val subject = kontainer {
            module(common)

            prototype(Base::class) { s1: S01, s2: S02 ->
                Config(s1.v + s2.v)
            }
        }.create()

        validateWithBase(subject, 11)
    }

    "Factory with three params" {

        val subject = kontainer {
            module(common)

            prototype(Config::class) { s1: S01, s2: S02, s3: S03 ->
                Config(s1.v + s2.v + s3.v)
            }
        }.create()

        validateWithoutBase(subject, 111)
    }

    "Factory with three params defined with base class" {

        val subject = kontainer {
            module(common)

            prototype(Base::class) { s1: S01, s2: S02, s3: S03 ->
                Config(s1.v + s2.v + s3.v)
            }
        }.create()

        validateWithBase(subject, 111)
    }

    "Factory with four params" {

        val subject = kontainer {
            module(common)

            prototype(Config::class) { s1: S01, s2: S02, s3: S03, s4: S04 ->
                Config(s1.v + s2.v + s3.v + s4.v)
            }
        }.create()

        validateWithoutBase(subject, 1111)
    }

    "Factory with four defined with base class" {

        val subject = kontainer {
            module(common)

            prototype(Base::class) { s1: S01, s2: S02, s3: S03, s4: S04 ->
                Config(s1.v + s2.v + s3.v + s4.v)
            }
        }.create()

        validateWithBase(subject, 1111)
    }

    "Factory with five params" {

        val subject = kontainer {
            module(common)

            prototype(Config::class) { s1: S01, s2: S02, s3: S03, s4: S04, s5: S05 ->
                Config(s1.v + s2.v + s3.v + s4.v + s5.v)
            }
        }.create()

        validateWithoutBase(subject, 11111)
    }

    "Factory with five params defined with base class" {

        val subject = kontainer {
            module(common)

            prototype(Base::class) { s1: S01, s2: S02, s3: S03, s4: S04, s5: S05 ->
                Config(s1.v + s2.v + s3.v + s4.v + s5.v)
            }
        }.create()

        validateWithBase(subject, 11111)
    }

    "Factory with six params" {

        val subject = kontainer {
            module(common)

            prototype(Config::class) { s1: S01, s2: S02, s3: S03, s4: S04, s5: S05, s6: S06 ->
                Config(s1.v + s2.v + s3.v + s4.v + s5.v + s6.v)
            }
        }.create()

        validateWithoutBase(subject, 111111)
    }

    "Factory with six params defined with base class" {

        val subject = kontainer {
            module(common)

            prototype(Base::class) { s1: S01, s2: S02, s3: S03, s4: S04, s5: S05, s6: S06 ->
                Config(s1.v + s2.v + s3.v + s4.v + s5.v + s6.v)
            }
        }.create()

        validateWithBase(subject, 111111)
    }

    "Factory with seven params" {

        val subject = kontainer {
            module(common)

            prototype(Config::class) { s1: S01, s2: S02, s3: S03, s4: S04, s5: S05, s6: S06, s7: S07 ->
                Config(s1.v + s2.v + s3.v + s4.v + s5.v + s6.v + s7.v)
            }
        }.create()

        validateWithoutBase(subject, 1111111)
    }

    "Factory with seven params defined with base class" {

        val subject = kontainer {
            module(common)

            prototype(Base::class) { s1: S01, s2: S02, s3: S03, s4: S04, s5: S05, s6: S06, s7: S07 ->
                Config(s1.v + s2.v + s3.v + s4.v + s5.v + s6.v + s7.v)
            }
        }.create()

        validateWithBase(subject, 1111111)
    }

    "Factory with eight params" {

        val subject = kontainer {
            module(common)

            prototype(Config::class) { s1: S01, s2: S02, s3: S03, s4: S04, s5: S05, s6: S06, s7: S07, s8: S08 ->
                Config(s1.v + s2.v + s3.v + s4.v + s5.v + s6.v + s7.v + s8.v)
            }
        }.create()

        validateWithoutBase(subject, 11111111)
    }

    "Factory with eight params defined with base class" {

        val subject = kontainer {
            module(common)

            prototype(Base::class) { s1: S01, s2: S02, s3: S03, s4: S04, s5: S05, s6: S06, s7: S07, s8: S08 ->
                Config(s1.v + s2.v + s3.v + s4.v + s5.v + s6.v + s7.v + s8.v)
            }
        }.create()

        validateWithBase(subject, 11111111)
    }

    "Factory with nine params" {

        val subject = kontainer {
            module(common)

            prototype(Config::class) { s1: S01, s2: S02, s3: S03, s4: S04, s5: S05, s6: S06, s7: S07, s8: S08, s9: S09 ->
                Config(s1.v + s2.v + s3.v + s4.v + s5.v + s6.v + s7.v + s8.v + s9.v)
            }
        }.create()

        validateWithoutBase(subject, 111111111)
    }

    "Factory with nine params defined with base class" {

        val subject = kontainer {
            module(common)

            prototype(Base::class) { s1: S01, s2: S02, s3: S03, s4: S04, s5: S05, s6: S06, s7: S07, s8: S08, s9: S09 ->
                Config(s1.v + s2.v + s3.v + s4.v + s5.v + s6.v + s7.v + s8.v + s9.v)
            }
        }.create()

        validateWithBase(subject, 111111111)
    }

    "Factory with ten params" {

        val subject = kontainer {
            module(common)

            prototype(Config::class) {
                    s1: S01, s2: S02, s3: S03, s4: S04, s5: S05, s6: S06, s7: S07, s8: S08, s9: S09,
                    s10: S10,
                ->
                Config(s1.v + s2.v + s3.v + s4.v + s5.v + s6.v + s7.v + s8.v + s9.v + s10.v)
            }
        }.create()

        validateWithoutBase(subject, 1111111111)
    }

    "Factory with ten params defined with base class" {

        val subject = kontainer {
            module(common)

            prototype(Base::class) {
                    s1: S01, s2: S02, s3: S03, s4: S04, s5: S05, s6: S06, s7: S07, s8: S08, s9: S09,
                    s10: S10,
                ->
                Config(s1.v + s2.v + s3.v + s4.v + s5.v + s6.v + s7.v + s8.v + s9.v + s10.v)
            }
        }.create()

        validateWithBase(subject, 1111111111)
    }

    "Factory with one params defined via dynamic function" {

        val blueprint = kontainer {
            module(common)

            val provider = { s1: S01 -> Config(s1.v) }

            prototype(Base::class, provider)
        }

        val subject = blueprint.create()

        validateWithBase(subject, 1)
    }

    "Factory with seven params defined via dynamic function" {

        val blueprint = kontainer {
            module(common)

            val provider = { s1: S01, s2: S02, s3: S03, s4: S04, s5: S05, s6: S06, s7: S07 ->
                Config(s1.v + s2.v + s3.v + s4.v + s5.v + s6.v + s7.v)
            }

            prototype(Base::class, provider)
        }

        val subject = blueprint.create()

        validateWithBase(subject, 1111111)
    }

    "Factory with ten params defined via dynamic function" {

        val blueprint = kontainer {
            module(common)

            val provider =
                { s1: S01, s2: S02, s3: S03, s4: S04, s5: S05, s6: S06, s7: S07, s8: S08, s9: S09, s10: S10 ->
                    Config(s1.v + s2.v + s3.v + s4.v + s5.v + s6.v + s7.v + s8.v + s9.v + s10.v)
                }

            prototype(Base::class, provider)
        }

        val subject = blueprint.create()

        validateWithBase(subject, 1111111111)
    }
})
