package de.peekandpoke.ultra.kontainer.e2e

import de.peekandpoke.ultra.kontainer.AmbiguousImplOne
import de.peekandpoke.ultra.kontainer.AmbiguousImplTwo
import de.peekandpoke.ultra.kontainer.AnotherSimpleService
import de.peekandpoke.ultra.kontainer.CounterService
import de.peekandpoke.ultra.kontainer.InjectingAllAmbiguous
import de.peekandpoke.ultra.kontainer.InjectingAmbiguous
import de.peekandpoke.ultra.kontainer.KontainerInconsistent
import de.peekandpoke.ultra.kontainer.kontainer
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class BaseTypeInjectionSpec : StringSpec({

    abstract class MyService

    class MyServiceImpl : MyService()

    "Injecting a service by its base class" {

        val subject = kontainer {
            singleton(MyServiceImpl::class)
        }.create()

        assertSoftly {
            subject.get<MyService>()::class shouldBe MyServiceImpl::class
        }
    }

    "Service with an ambiguous dependency" {

        val blueprint = kontainer {
            singleton(InjectingAmbiguous::class)
            singleton(AmbiguousImplOne::class)
            singleton(AmbiguousImplTwo::class)
        }

        assertSoftly {
            val error = shouldThrow<KontainerInconsistent> {
                blueprint.create()
            }

            error.message shouldContain "Parameter 'ambiguous' is ambiguous"
        }
    }

    "Creating a container with a service that injects all services of a base type" {

        val subject = kontainer {
            singleton(InjectingAllAmbiguous::class)
            singleton(AmbiguousImplOne::class)
            singleton(AmbiguousImplTwo::class)

            singleton(CounterService::class)
            singleton(AnotherSimpleService::class)
        }.create()

        assertSoftly {

            subject.get<InjectingAllAmbiguous>()::class shouldBe InjectingAllAmbiguous::class

            val implOne = subject.get<AmbiguousImplOne>()
            val implTwo = subject.get<AmbiguousImplTwo>()

            subject.get<InjectingAllAmbiguous>().all shouldBe listOf(implOne, implTwo)
        }
    }
})
