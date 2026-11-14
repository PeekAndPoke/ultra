package io.peekandpoke.funktor.insights

import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.funktor.insights.collectors.KontainerCollector
import io.peekandpoke.ultra.kontainer.ServiceKind
import io.peekandpoke.ultra.kontainer.ServiceType
import io.peekandpoke.ultra.kontainer.kontainer

class WireStatic
class WireDyn
class WireSemi(val dyn: WireDyn)
class WireProto
class WireDynProto
class WireSemiProto(val dyn: WireDyn)
open class WireOverridden
class WireOverride : WireOverridden()

/**
 * Pins the wire names insights writes for Kontainer services: the five names records on disk and both GUIs read.
 */
class KontainerWireNamesSpec : StringSpec({

    "Every type and kind maps to one of the five wire names" {
        assertSoftly {
            wireProviderType(ServiceType.Singleton, ServiceKind.Static, false) shouldBe "Singleton"
            wireProviderType(ServiceType.Singleton, ServiceKind.SemiDynamic, false) shouldBe "SemiDynamic"
            wireProviderType(ServiceType.Singleton, ServiceKind.Dynamic, false) shouldBe "Dynamic"
            wireProviderType(ServiceType.Singleton, ServiceKind.Dynamic, true) shouldBe "DynamicOverride"
            wireProviderType(ServiceType.Prototype, ServiceKind.Static, false) shouldBe "Prototype"
            wireProviderType(ServiceType.Prototype, ServiceKind.SemiDynamic, false) shouldBe "Prototype"
            wireProviderType(ServiceType.Prototype, ServiceKind.Dynamic, false) shouldBe "Prototype"
        }
    }

    "A declared definition maps to one of the three wire names" {
        assertSoftly {
            wireInjectionType(ServiceType.Singleton, ServiceKind.Static) shouldBe "Singleton"
            wireInjectionType(ServiceType.Singleton, ServiceKind.Dynamic) shouldBe "Dynamic"
            wireInjectionType(ServiceType.Prototype, ServiceKind.Static) shouldBe "Prototype"
            wireInjectionType(ServiceType.Prototype, ServiceKind.Dynamic) shouldBe "Prototype"
        }
    }

    "End to end: the graph and the record slice carry the old names for every combination" {

        val blueprint = kontainer {
            singleton(WireStatic::class)
            dynamic(WireDyn::class)
            singleton(WireSemi::class)
            prototype(WireProto::class)
            dynamicPrototype(WireDynProto::class)
            prototype(WireSemiProto::class)
            dynamic(WireOverridden::class)
        }

        val graph = InsightsGraphs(blueprint).kontainerGraph

        fun graphType(cls: Class<*>) = graph.services.single { it.cls == cls.name }.type
        fun graphInjectionType(cls: Class<*>) = graph.services.single { it.cls == cls.name }.definition.injectionType

        val request = blueprint.create { with { WireOverride() } }
        listOf(WireStatic::class, WireSemi::class, WireProto::class, WireDynProto::class, WireSemiProto::class)
            .forEach { request.get(it) }

        val slice = KontainerCollector.Data.of(
            graphFingerprint = graph.fingerprint,
            providers = request.tools.factory.getAllProviders(),
        )

        fun sliceType(cls: Class<*>) = slice.services.single { it.cls == cls.name }.type

        assertSoftly {
            graphType(WireStatic::class.java) shouldBe "Singleton"
            graphType(WireDyn::class.java) shouldBe "Dynamic"
            graphType(WireSemi::class.java) shouldBe "SemiDynamic"
            graphType(WireProto::class.java) shouldBe "Prototype"
            graphType(WireDynProto::class.java) shouldBe "Prototype"
            graphType(WireSemiProto::class.java) shouldBe "Prototype"
            graphType(WireOverridden::class.java) shouldBe "Dynamic"

            graphInjectionType(WireSemi::class.java) shouldBe "Singleton"
            graphInjectionType(WireDyn::class.java) shouldBe "Dynamic"
            graphInjectionType(WireDynProto::class.java) shouldBe "Prototype"

            sliceType(WireStatic::class.java) shouldBe "Singleton"
            sliceType(WireDyn::class.java) shouldBe "Dynamic"
            sliceType(WireSemi::class.java) shouldBe "SemiDynamic"
            sliceType(WireProto::class.java) shouldBe "Prototype"
            sliceType(WireDynProto::class.java) shouldBe "Prototype"
            sliceType(WireSemiProto::class.java) shouldBe "Prototype"
            sliceType(WireOverridden::class.java) shouldBe "DynamicOverride"
        }
    }

    "An override that was never requested has its instance, so it is in the record slice (ULTRA-00344)" {

        val blueprint = kontainer {
            dynamic(WireOverridden::class)
        }

        val request = blueprint.create { with { WireOverride() } }

        val slice = KontainerCollector.Data.of(
            graphFingerprint = null,
            providers = request.tools.factory.getAllProviders(),
        )

        slice.services.single { it.cls == WireOverridden::class.java.name }.type shouldBe "DynamicOverride"
    }
})
