package io.peekandpoke.ultra.kontainer.e2e

import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.kontainer.InjectionContext
import io.peekandpoke.ultra.kontainer.Kontainer
import io.peekandpoke.ultra.kontainer.KontainerBlueprint
import io.peekandpoke.ultra.kontainer.domain.DebugInfo
import io.peekandpoke.ultra.kontainer.getName
import io.peekandpoke.ultra.kontainer.kontainer
import kotlin.reflect.KClass

class KindsStatic
class KindsDyn
class KindsSemi(val dyn: KindsDyn)
class KindsProto
class KindsDynProto
class KindsSemiProto(val dynProto: KindsDynProto)
class KindsSemiTwo(val dyn: KindsDyn, val dynProto: KindsDynProto)
open class KindsOverridden
class KindsOverride : KindsOverridden()

class ChainDyn
class ChainDynAbove(val dyn: ChainDyn)
class ChainMid(val dyn: ChainDyn)
class ChainProtoMid(val dyn: ChainDyn)
class ChainViaSingleton(val mid: ChainMid)
class ChainViaPrototype(val mid: ChainProtoMid)
class ChainViaDynamic(val above: ChainDynAbove)

/**
 * Pins what the tools report for each of the six type and kind combinations, plus an overridden dynamic.
 */
class KontainerToolsKindsSpec : StringSpec() {

    private val blueprint = kontainer {
        singleton(KindsStatic::class)
        dynamic(KindsDyn::class)
        singleton(KindsSemi::class)
        prototype(KindsProto::class)
        dynamicPrototype(KindsDynProto::class)
        prototype(KindsSemiProto::class)
        singleton(KindsSemiTwo::class)
        dynamic(KindsOverridden::class)
    }

    private fun subject(): Kontainer = blueprint.create { with { KindsOverride() } }

    private fun DebugInfo.service(cls: KClass<*>): DebugInfo.ServiceDebugInfo =
        services.first { it.cls.fqn == cls.java.name }

    private val DebugInfo.ServiceDebugInfo.reported get() = Reported(type, kind, overridden)

    init {

        "Debug info: type, kind and the override flag of every service" {

            val info = subject().tools.getDebugInfo()

            info.services.associate { it.cls.fqn to it.reported } shouldBe mapOf(
                KindsStatic::class.java.name to Reported.StaticSingleton,
                KindsDyn::class.java.name to Reported.DynamicSingleton,
                KindsSemi::class.java.name to Reported.SemiDynamicSingleton,
                KindsProto::class.java.name to Reported.StaticPrototype,
                KindsDynProto::class.java.name to Reported.DynamicPrototype,
                KindsSemiProto::class.java.name to Reported.SemiDynamicPrototype,
                KindsSemiTwo::class.java.name to Reported.SemiDynamicSingleton,
                KindsOverridden::class.java.name to Reported.OverriddenDynamicSingleton,
                Kontainer::class.java.name to Reported.DynamicSingleton,
                InjectionContext::class.java.name to Reported.DynamicSingleton,
                KontainerBlueprint::class.java.name to Reported.StaticSingleton,
            )
        }

        "Debug info: the listing order is static, semi dynamic and dynamic singletons, then prototypes" {

            val info = subject().tools.getDebugInfo()

            info.services.map { it.cls.fqn } shouldBe listOf(
                KindsStatic::class,
                KontainerBlueprint::class,
                KindsSemi::class,
                KindsSemiTwo::class,
                KindsDyn::class,
                KindsOverridden::class,
                Kontainer::class,
                InjectionContext::class,
                KindsProto::class,
                KindsDynProto::class,
                KindsSemiProto::class,
            ).map { it.java.name }
        }

        "Debug info: a definition carries the declared type and kind, never SemiDynamic" {

            val info = subject().tools.getDebugInfo()

            fun declared(cls: KClass<*>) = info.service(cls).definition.let { Reported(it.type, it.kind) }

            assertSoftly {
                declared(KindsStatic::class) shouldBe Reported.StaticSingleton
                declared(KindsSemi::class) shouldBe Reported.StaticSingleton
                declared(KindsDyn::class) shouldBe Reported.DynamicSingleton
                declared(KindsProto::class) shouldBe Reported.StaticPrototype
                declared(KindsSemiProto::class) shouldBe Reported.StaticPrototype
                declared(KindsDynProto::class) shouldBe Reported.DynamicPrototype

                declared(KindsOverridden::class) shouldBe Reported.DynamicSingleton
                val overwrites = info.service(KindsOverridden::class).definition.overwrites.shouldNotBeNull()
                Reported(overwrites.type, overwrites.kind) shouldBe Reported.DynamicSingleton
                overwrites.creates.fqn shouldBe KindsOverridden::class.java.name
            }
        }

        "Debug info: a SemiDynamic service names the declared dynamic services it reaches" {

            val info = subject().tools.getDebugInfo()

            fun because(cls: KClass<*>) = info.service(cls).semiDynamicBecauseOf.map { it.fqn }

            assertSoftly {
                because(KindsSemi::class) shouldBe listOf(KindsDyn::class.java.name)
                because(KindsSemiProto::class) shouldBe listOf(KindsDynProto::class.java.name)
                because(KindsSemiTwo::class) shouldBe listOf(KindsDyn::class.java.name, KindsDynProto::class.java.name)

                because(KindsStatic::class) shouldBe emptyList()
                because(KindsDyn::class) shouldBe emptyList()
                because(KindsDynProto::class) shouldBe emptyList()
                because(KindsProto::class) shouldBe emptyList()
                because(KindsOverridden::class) shouldBe emptyList()
            }
        }

        "A SemiDynamic service names the dynamic services it reaches through any chain" {

            val subject = kontainer {
                dynamic(ChainDyn::class)
                dynamic(ChainDynAbove::class)
                singleton(ChainMid::class)
                prototype(ChainProtoMid::class)
                singleton(ChainViaSingleton::class)
                singleton(ChainViaPrototype::class)
                singleton(ChainViaDynamic::class)
            }.create()

            val info = subject.tools.getDebugInfo()

            fun because(cls: KClass<*>) = info.service(cls).semiDynamicBecauseOf.map { it.fqn }

            assertSoftly {
                because(ChainViaSingleton::class) shouldBe listOf(ChainDyn::class.java.name)
                because(ChainViaPrototype::class) shouldBe listOf(ChainDyn::class.java.name)
                because(ChainViaDynamic::class) shouldBe
                        listOf(ChainDyn::class.java.name, ChainDynAbove::class.java.name)

                because(ChainDynAbove::class) shouldBe emptyList()
                because(ChainDyn::class) shouldBe emptyList()
            }
        }

        "Dump: a header and one row per service with its type and kind" {

            val lines = subject().tools.dumpKontainer().lines()

            fun row(cls: KClass<*>): List<String> =
                lines.first { it.startsWith(cls.getName() + " ") }.split("|").map { it.trim() }.subList(1, 3)

            assertSoftly {
                lines[0] shouldBe "Kontainer dump:"
                lines[2].split("|").map { it.trim() } shouldBe listOf("Service ID", "Type", "Kind", "Instances")

                row(KindsStatic::class) shouldBe listOf("Singleton", "Static")
                row(KindsDyn::class) shouldBe listOf("Singleton", "Dynamic")
                row(KindsSemi::class) shouldBe listOf("Singleton", "SemiDynamic (KindsDyn)")
                row(KindsProto::class) shouldBe listOf("Prototype", "Static")
                row(KindsDynProto::class) shouldBe listOf("Prototype", "Dynamic")
                row(KindsSemiProto::class) shouldBe listOf("Prototype", "SemiDynamic (KindsDynProto)")
                row(KindsSemiTwo::class) shouldBe listOf("Singleton", "SemiDynamic (KindsDyn, KindsDynProto)")
                row(KindsOverridden::class) shouldBe listOf("Singleton", "Dynamic (overridden)")
            }
        }
    }
}
