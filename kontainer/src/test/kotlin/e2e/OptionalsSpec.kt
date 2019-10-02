package de.peekandpoke.ultra.kontainer.e2e

import de.peekandpoke.ultra.kontainer.kontainer
import io.kotlintest.assertSoftly
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec

class OptionalsSpec : StringSpec({

    "Using Kontainer::use() when a service is present" {

        class Service {
            val x = 100
        }

        val subject = kontainer {
            singleton(Service::class)
        }.useWith()

        assertSoftly {
            subject.use(Service::class) { x * 100 } shouldBe 100 * 100
        }
    }

    "Using Kontainer::use() when a service is missing" {

        class Service {
            val x = 100
        }

        val subject = kontainer {}.useWith()

        assertSoftly {
            subject.use(Service::class) { x * 100 } shouldBe null
        }
    }
})
