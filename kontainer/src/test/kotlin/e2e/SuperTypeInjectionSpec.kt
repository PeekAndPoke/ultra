package de.peekandpoke.ultra.kontainer.e2e

import de.peekandpoke.ultra.kontainer.kontainer
import io.kotlintest.assertSoftly
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec

class SuperTypeInjectionSpec : StringSpec({

    "Creating a container with a service that injects all services that have a certain supertype" {

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
