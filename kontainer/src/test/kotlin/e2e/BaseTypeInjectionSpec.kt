package de.peekandpoke.ultra.kontainer.e2e

import de.peekandpoke.ultra.kontainer.*
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

    "Creating a container with a service that injects all services of a base type" {

        val subject = kontainer {
            singleton<InjectingAllAmbiguous>()
            singleton<AmbiguousImplOne>()
            singleton<AmbiguousImplTwo>()

            singleton<SimpleService>()
            singleton<AnotherSimpleService>()
        }.useWith()

        assertSoftly {

            subject.get<InjectingAllAmbiguous>()::class shouldBe InjectingAllAmbiguous::class

            val implOne = subject.get<AmbiguousImplOne>()
            val implTwo = subject.get<AmbiguousImplTwo>()

            subject.get<InjectingAllAmbiguous>().all shouldBe listOf(implOne, implTwo)
        }
    }
})
