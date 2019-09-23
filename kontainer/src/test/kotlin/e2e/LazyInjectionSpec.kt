package de.peekandpoke.ultra.kontainer.e2e

import de.peekandpoke.ultra.kontainer.LazyImpl
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

            subject.get<LazilyInjecting>().lazy::class shouldBe LazyImpl::class

            subject.get<LazilyInjecting>().lazy.value.get() shouldBe 1
        }
    }
})
