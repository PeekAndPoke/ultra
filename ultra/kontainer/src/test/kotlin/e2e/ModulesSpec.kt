package de.peekandpoke.ultra.kontainer.e2e

import de.peekandpoke.ultra.kontainer.KontainerDslModule
import de.peekandpoke.ultra.kontainer.KontainerModule
import de.peekandpoke.ultra.kontainer.kontainer
import de.peekandpoke.ultra.kontainer.module
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ModulesSpec : StringSpec({

    "Creating a module" {

        val subject = module {
            singleton(CounterService::class)
        }

        subject::class shouldBe KontainerModule::class
    }

    "Creating and using a module" {

        val moduleOne = module {
            singleton(CounterService::class)
        }

        val moduleTwo = module {
            singleton(AnotherSimpleService::class)
        }

        val kontainer = kontainer {
            module(moduleOne)
            module(moduleTwo)
            singleton(InjectingService::class)
        }.create()

        assertSoftly {
            kontainer.get<InjectingService>().let {
                it::class shouldBe InjectingService::class
                it.simple::class shouldBe CounterService::class
                it.another::class shouldBe AnotherSimpleService::class
            }
        }
    }

    "Creating and using a parameterized module" {

        data class MyService(val value: Int)

        @KontainerDslModule
        val mod = module { config: Int ->
            instance(MyService(config))
        }

        val subject = kontainer {
            module(mod, 100)
        }.create()

        assertSoftly {
            subject.get(MyService::class).value shouldBe 100
        }
    }

    "Creating and using a parameterized module with two parameters" {

        data class MyService(val value: Int)

        val mod = module { config1: Int, config2: Int ->
            instance(MyService(config1 + config2))
        }

        val subject = kontainer {
            module(mod, 100, 200)
        }.create()

        assertSoftly {
            subject.get(MyService::class).value shouldBe 300
        }
    }

    "Creating and using a parameterized module with three parameters" {

        data class MyService(val value: Int)

        val mod = module { config1: Int, config2: Int, config3: Int ->
            instance(MyService(config1 + config2 + config3))
        }

        val subject = kontainer {
            module(mod, 100, 200, 300)
        }.create()

        assertSoftly {
            subject.get(MyService::class).value shouldBe 600
        }
    }
})
