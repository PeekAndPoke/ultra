package de.peekandpoke.ultra.kontainer.e2e

import de.peekandpoke.ultra.kontainer.KontainerInconsistent
import de.peekandpoke.ultra.kontainer.kontainer
import io.kotlintest.assertSoftly
import io.kotlintest.matchers.string.shouldContain
import io.kotlintest.shouldBe
import io.kotlintest.shouldThrow
import io.kotlintest.specs.StringSpec

class BaseTypeInjectionSpec : StringSpec({

    abstract class MyService

    class MyServiceImpl : MyService()

    "Injecting a service by its base class" {

        val subject = kontainer {
            singleton<MyServiceImpl>()
        }.useWith()

        assertSoftly {
            subject.get<MyService>()::class shouldBe MyServiceImpl::class
        }
    }

    "Service with an ambiguous dependency" {

        val blueprint = kontainer {
            singleton<InjectingAmbiguous>()
            singleton<AmbiguousImplOne>()
            singleton<AmbiguousImplTwo>()
        }

        assertSoftly {
            val error = shouldThrow<KontainerInconsistent> {
                blueprint.useWith()
            }

            error.message shouldContain "Parameter 'ambiguous' is ambiguous"
        }
    }
})
