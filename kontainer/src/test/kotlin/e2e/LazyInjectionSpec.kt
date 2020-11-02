package de.peekandpoke.ultra.kontainer.e2e

import de.peekandpoke.ultra.common.Lookup
import de.peekandpoke.ultra.kontainer.LazilyInjecting
import de.peekandpoke.ultra.kontainer.LazyServiceLookup
import de.peekandpoke.ultra.kontainer.SimpleService
import de.peekandpoke.ultra.kontainer.kontainer
import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class LazyInjectionSpec : StringSpec({

    "Injecting a service lazily" {

        val subject = kontainer {
            singleton<SimpleService>()
            singleton<LazilyInjecting>()
        }.useWith()

        subject.get<SimpleService>().inc()

        assertSoftly {
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
            subject.get(Injecting::class).all.value.map { it::class } shouldBe listOf(ImplOne::class, ImplTwo::class)
        }
    }

    "Injecting a lazy lookup of Services" {

        abstract class Base
        class ImplOne : Base()
        class ImplTwo : Base()

        data class Injecting(val all: Lookup<Base>)

        val subject = kontainer {
            singleton(ImplOne::class)
            singleton(ImplTwo::class)

            singleton(Injecting::class)
        }.useWith()

        val implOne = subject.get(ImplOne::class)
        val implTwo = subject.get(ImplTwo::class)

        val injecting = subject.get(Injecting::class)

        assertSoftly {
            injecting.all::class shouldBe LazyServiceLookup::class

            injecting.all.get(ImplOne::class) shouldBe implOne
            injecting.all.get(ImplTwo::class) shouldBe implTwo

            injecting.all.all().map { it::class } shouldBe listOf(ImplOne::class, ImplTwo::class)
        }
    }
})
