package de.peekandpoke.ultra.kontainer.e2e

import de.peekandpoke.ultra.kontainer.*
import io.kotlintest.assertSoftly
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec

class ModulesSpec : StringSpec({

    "Creating a module" {

        val subject = module {
            singleton<SimpleService>()
        }

        subject::class shouldBe KontainerModule::class
    }

    "Creating and using a module" {

        val moduleOne = module {
            config("configInt", 100)
            singleton<SimpleService>()
        }

        val moduleTwo = module {
            singleton<AnotherSimpleService>()
        }

        val kontainer = kontainer {
            module(moduleOne)
            module(moduleTwo)
            singleton<InjectingService>()
            singleton<ConfigIntInjecting>()
        }.useWith()

        assertSoftly {
            kontainer.get<InjectingService>()::class shouldBe InjectingService::class

            kontainer.get<ConfigIntInjecting>().configInt shouldBe 100
        }
    }

    "Creating and using a parameterized module" {

        data class MyService(val value: Int)

        val mod = module { config: Int ->
            instance(MyService(config))
        }

        val subject = kontainer {
            module(mod, 100)
        }.useWith()

        assertSoftly {
            subject.get(MyService::class).value shouldBe 100
        }
    }
})
