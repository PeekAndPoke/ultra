package de.peekandpoke.ultra.kontainer.e2e

import de.peekandpoke.ultra.kontainer.kontainer
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class OptionalsSpec : StringSpec({

    "Using Kontainer::use() when a service is present" {

        class Service {
            val x = 100
        }

        val subject = kontainer {
            singleton(Service::class)
        }.create()

        assertSoftly {
            subject.use(Service::class) { x * 100 } shouldBe 100 * 100
        }
    }

    "Using Kontainer::use() when a service is missing" {

        class Service {
            val x = 100
        }

        val subject = kontainer {}.create()

        assertSoftly {
            subject.use(Service::class) { x * 100 } shouldBe null
        }
    }

    "Injecting a missing service that is marked as nullable" {

        class Service
        class Injecting(val service: Service?)

        val subject = kontainer {
            singleton(Injecting::class)
        }.create()

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
        }.create()

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
        }.create()

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
        }.create()

        assertSoftly {
            val injecting = subject.get(Injecting::class)

            injecting.service shouldNotBe null
            injecting.service?.value shouldNotBe null
        }
    }
})
