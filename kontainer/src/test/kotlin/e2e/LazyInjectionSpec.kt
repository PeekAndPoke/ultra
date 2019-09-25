package de.peekandpoke.ultra.kontainer.e2e

import de.peekandpoke.ultra.common.SimpleLazy
import de.peekandpoke.ultra.kontainer.LazilyInjecting
import de.peekandpoke.ultra.kontainer.SimpleService
import de.peekandpoke.ultra.kontainer.kontainer
import io.kotlintest.assertSoftly
import io.kotlintest.shouldBe
import io.kotlintest.specs.StringSpec

class LazyInjectionSpec : StringSpec({

    "Injecting a service lazily" {

        val subject = kontainer {
            singleton<SimpleService>()
            singleton<LazilyInjecting>()
        }.useWith()

        subject.get<SimpleService>().inc()

        assertSoftly {

            subject.get<LazilyInjecting>().lazy::class shouldBe SimpleLazy::class

            subject.get<LazilyInjecting>().lazy.value.get() shouldBe 1
        }
    }

    "Injecting a lazy list of Services" {

        abstract class Base
        class ImplOne : Base()
        class ImplTwo : Base()

        data class Injecting(val all: Lazy<List<Base>>)

        val subject = kontainer {
            singleton(ImplOne::class)
            singleton(ImplTwo::class)

            singleton(Injecting::class)
        }.useWith()

        assertSoftly {
            subject.get(Injecting::class).all::class shouldBe SimpleLazy::class

            subject.get(Injecting::class).all.value.map { it::class } shouldBe listOf(ImplOne::class, ImplTwo::class)
        }
    }
})
