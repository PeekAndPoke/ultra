package io.peekandpoke.ultra.kontainer.e2e

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.kontainer.ServiceProviderFactory
import kotlin.reflect.KVisibility

/**
 * Pins that no public API constructs a [ServiceProviderFactory], hands out its override lambdas (ULTRA-00346) or makes
 * a kontainer from it outside `create` and `clone`.
 */
class ServiceProviderFactoryVisibilitySpec : StringSpec({

    "The ServiceProviderFactory constructor is internal" {

        ServiceProviderFactory::class.constructors.map { it.visibility }.toSet() shouldBe setOf(KVisibility.INTERNAL)
    }

    "The override lambdas of a factory are not public" {

        ServiceProviderFactory::dynamics.visibility shouldBe KVisibility.INTERNAL
    }

    "A factory makes new kontainers only through Kontainer.clone" {

        ServiceProviderFactory::newKontainer.visibility shouldBe KVisibility.INTERNAL
    }
})
