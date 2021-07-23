package de.peekandpoke.ultra.kontainer.e2e

import de.peekandpoke.ultra.kontainer.ConfigAllInjecting
import de.peekandpoke.ultra.kontainer.ConfigIntInjecting
import de.peekandpoke.ultra.kontainer.KontainerInconsistent
import de.peekandpoke.ultra.kontainer.kontainer
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class ConfigInjectionSpec : StringSpec({

    "Injecting a missing config value" {

        val blueprint = kontainer {
            singleton<ConfigIntInjecting>()
        }

        assertSoftly {

            val error = shouldThrow<KontainerInconsistent> {
                blueprint.create()
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
                blueprint.create()
            }

            error.message shouldContain ConfigIntInjecting::class.qualifiedName!!

            error.message shouldContain "Parameter 'configInt'"
        }
    }

    "Injecting all kinds of  config values" {

        val blueprint = kontainer {
            config("int", 1)
            config("long", 2L)
            config("float", 3.0f)
            config("double", 4.0)
            config("string", "TEXT")
            config("boolean", true)

            singleton<ConfigAllInjecting>()
        }

        val subject = blueprint.create()

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
