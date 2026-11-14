package io.peekandpoke.ultra.kontainer.e2e

import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.kontainer.ServiceProviderFactory
import kotlin.reflect.KVisibility

/**
 * Pins that no public API constructs a [ServiceProviderFactory] or hands out its override lambdas (ULTRA-00346).
 */
class ServiceProviderFactoryVisibilitySpec : StringSpec({

    "The ServiceProviderFactory constructor is internal" {

        ServiceProviderFactory::class.constructors.map { it.visibility }.toSet() shouldBe setOf(KVisibility.INTERNAL)
    }

    "The override lambdas of a factory are not public" {

        assertSoftly {
            ServiceProviderFactory::dynamics.visibility shouldBe KVisibility.INTERNAL
        }
    }
})
