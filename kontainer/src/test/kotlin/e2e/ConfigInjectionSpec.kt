package de.peekandpoke.ultra.kontainer.e2e

import de.peekandpoke.ultra.kontainer.KontainerInconsistent
import de.peekandpoke.ultra.kontainer.kontainer
import io.kotlintest.assertSoftly
import io.kotlintest.matchers.string.shouldContain
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import io.kotlintest.specs.StringSpec

class ConfigInjectionSpec : StringSpec({

    "Injecting a missing config value" {

        val blueprint = kontainer {
            singleton<ConfigIntInjecting>()
        }

        assertSoftly {

            val error = shouldThrow<KontainerInconsistent> {
                blueprint.useWith()
            }

            error.message shouldContain ConfigIntInjecting::class.qualifiedName!!

            error.message shouldContain "Parameter 'configInt'"
        }
    }

    "Injecting a config value of wrong type" {

        val blueprint = kontainer {
            config("configInt", "this is no int")
            singleton<ConfigIntInjecting>()
        }

        assertSoftly {

            val error = shouldThrow<KontainerInconsistent> {
                blueprint.useWith()
            }

            error.message shouldContain ConfigIntInjecting::class.qualifiedName!!

            error.message shouldContain "Parameter 'configInt'"
        }
    }

    "Injecting all kinds of  config values" {

        val subject = kontainer {
            config("int", 1)
            config("long", 2L)
            config("float", 3.0f)
            config("double", 4.0)
            config("string", "TEXT")
            config("boolean", true)

            singleton<ConfigAllInjecting>()

        }.useWith()

        assertSoftly {

            val service = subject.get<ConfigAllInjecting>()

            service.int shouldBe 1
            service.long shouldBe 2L
            service.float shouldBe 3.0f
            service.double shouldBe 4.0
            service.string shouldBe "TEXT"
            service.boolean shouldBe true
        }
    }
})
