package io.peekandpoke.ultra.kontainer.e2e

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.kontainer.ServiceDefinition
import io.peekandpoke.ultra.kontainer.ServiceKind
import io.peekandpoke.ultra.kontainer.ServiceProducer
import io.peekandpoke.ultra.kontainer.ServiceProvider
import io.peekandpoke.ultra.kontainer.ServiceType

/**
 * Pins the guards against type and kind combinations that mean nothing.
 */
class ScopeGuardsSpec : StringSpec({

    class Service

    fun definition(type: ServiceType, kind: ServiceKind) = ServiceDefinition(
        serviceCls = Service::class,
        type = type,
        kind = kind,
        producer = ServiceProducer.forInstance(Service()),
        overwrites = null,
    )

    "A definition cannot be declared SemiDynamic" {

        shouldThrow<IllegalArgumentException> {
            definition(ServiceType.Singleton, ServiceKind.SemiDynamic)
        }.message shouldBe "SemiDynamic cannot be declared, it is derived by the blueprint"
    }

    "A per-kontainer singleton provider cannot report Static" {

        shouldThrow<IllegalArgumentException> {
            ServiceProvider.Provider.forDynamicSingleton(
                kind = ServiceKind.Static,
                definition = definition(ServiceType.Singleton, ServiceKind.Static),
            )
        }.message shouldBe "A per-kontainer singleton is Dynamic or SemiDynamic, not Static"
    }

    "An override provider always reports a dynamic singleton that is overridden" {

        val provider = ServiceProvider.Provider.forOverride(
            definition = definition(ServiceType.Singleton, ServiceKind.Dynamic),
            instance = Service(),
        ).provide()

        provider.reported shouldBe Reported.OverriddenDynamicSingleton
    }
})
