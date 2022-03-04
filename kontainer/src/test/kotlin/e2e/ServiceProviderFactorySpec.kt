package de.peekandpoke.ultra.kontainer.e2e

import de.peekandpoke.ultra.kontainer.AnotherSimpleService
import de.peekandpoke.ultra.kontainer.CounterService
import de.peekandpoke.ultra.kontainer.SomeIndependentService
import de.peekandpoke.ultra.kontainer.kontainer
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class ServiceProviderFactorySpec : StringSpec({

    "Service providers must only be created on demand" {

        val kontainer = kontainer {
            instance(AnotherSimpleService())
            singleton(CounterService::class)
            dynamic(SomeIndependentService::class)
        }.create()

        val factory = kontainer.getFactory()

        assertSoftly {
            factory.isProviderCreated(AnotherSimpleService::class) shouldBe false
            factory.isProviderCreated(CounterService::class) shouldBe false
            factory.isProviderCreated(SomeIndependentService::class) shouldBe false

            kontainer.get(AnotherSimpleService::class)

            factory.isProviderCreated(AnotherSimpleService::class) shouldBe true
            factory.isProviderCreated(CounterService::class) shouldBe false
            factory.isProviderCreated(SomeIndependentService::class) shouldBe false

            kontainer.get(SomeIndependentService::class)

            factory.isProviderCreated(AnotherSimpleService::class) shouldBe true
            factory.isProviderCreated(CounterService::class) shouldBe false
            factory.isProviderCreated(SomeIndependentService::class) shouldBe true

            kontainer.get(CounterService::class)

            factory.isProviderCreated(AnotherSimpleService::class) shouldBe true
            factory.isProviderCreated(CounterService::class) shouldBe true
            factory.isProviderCreated(SomeIndependentService::class) shouldBe true
        }
    }
})
