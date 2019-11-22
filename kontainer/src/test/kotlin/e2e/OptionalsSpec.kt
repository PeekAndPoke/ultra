package de.peekandpoke.ultra.kontainer.e2e

import de.peekandpoke.ultra.kontainer.kontainer
import io.kotlintest.assertSoftly
import io.kotlintest.shouldBe
import io.kotlintest.shouldNotBe
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

    "Injecting a missing service that is marked as nullable" {

        class Service
        class Injecting(val service: Service?)

        val subject = kontainer {
            singleton(Injecting::class)
        }.useWith()

        assertSoftly {
            val injecting = subject.get(Injecting::class)

            injecting.service shouldBe null
        }
    }

    "Injecting an existing service that is marked as nullable" {

        class Service
        class Injecting(val service: Service?)

        val subject = kontainer {
            singleton(Service::class)
            singleton(Injecting::class)
        }.useWith()

        assertSoftly {
            val injecting = subject.get(Injecting::class)

            injecting.service shouldNotBe null
        }
    }

    "Injecting a missing service lazily that is marked as nullable" {

        class Service
        class Injecting(val service: Lazy<Service?>)

        val subject = kontainer {
            singleton(Injecting::class)
        }.useWith()

        assertSoftly {
            val injecting = subject.get(Injecting::class)

            injecting.service.value shouldBe null
        }
    }

    "Injecting an existing service lazily that is marked as nullable" {

        class Service
        class Injecting(val service: Lazy<Service>?)

        val subject = kontainer {
            singleton(Service::class)
            singleton(Injecting::class)
        }.useWith()

        assertSoftly {
            val injecting = subject.get(Injecting::class)

            injecting.service shouldNotBe null
            injecting.service?.value shouldNotBe null
        }
    }
})
