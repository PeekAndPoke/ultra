package io.peekandpoke.ultra.kontainer.e2e.characterization

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldStartWith
import io.peekandpoke.ultra.common.Lookup
import io.peekandpoke.ultra.kontainer.InjectionContext
import io.peekandpoke.ultra.kontainer.Kontainer
import io.peekandpoke.ultra.kontainer.KontainerBlueprint
import io.peekandpoke.ultra.kontainer.ServiceKind
import io.peekandpoke.ultra.kontainer.ServiceType
import io.peekandpoke.ultra.kontainer.domain.DebugInfo
import io.peekandpoke.ultra.kontainer.domain.DebugInfo.ParamInfo.ProvisionType
import io.peekandpoke.ultra.kontainer.e2e.Reported
import io.peekandpoke.ultra.kontainer.getName
import io.peekandpoke.ultra.kontainer.kontainer
import kotlin.reflect.KClass

abstract class ToolsBase
class ToolsSingle : ToolsBase()
class ToolsProto
class ToolsDyn : ToolsBase()
class ToolsPromoted(val dyn: ToolsDyn)
open class ToolsOverridden
class ToolsOverride : ToolsOverridden()

class ToolsForms(
    val plain: ToolsSingle,
    val nullable: ToolsProto?,
    val lazy: Lazy<ToolsSingle>,
    val list: List<ToolsBase>,
    val lazyList: Lazy<List<ToolsBase>>,
    val lookup: Lookup<ToolsBase>,
    val context: InjectionContext,
)

/**
 * Pins what [Kontainer.tools] report for one blueprint holding one service of each provider type of the old model.
 */
class ToolsCharacterizationSpec : StringSpec() {

    private val blueprint = kontainer {
        singleton(ToolsSingle::class)
        prototype(ToolsProto::class)
        dynamic(ToolsDyn::class)
        singleton(ToolsPromoted::class)
        dynamic(ToolsOverridden::class)
        prototype(ToolsForms::class)
    }

    private fun createAndUse(): Kontainer = blueprint.create { with { ToolsOverride() } }.apply {
        get(ToolsSingle::class)
        get(ToolsProto::class)
        get(ToolsProto::class)
        get(ToolsDyn::class)
        get(ToolsPromoted::class)
        get(ToolsOverridden::class)
    }

    private fun DebugInfo.service(cls: KClass<*>): DebugInfo.ServiceDebugInfo =
        services.first { it.cls.fqn == cls.java.name }

    init {

        "The enums the tools report with" {
            assertSoftly {
                ServiceType.entries.map { it.name } shouldBe listOf("Singleton", "Prototype")
                ServiceKind.entries.map { it.name } shouldBe listOf("Static", "Dynamic", "SemiDynamic")
                ProvisionType.entries.map { it.name } shouldBe listOf("Direct", "Lazy")
            }
        }

        "Debug info: the type of every service, the built-in ones included" {

            val info = createAndUse().tools.getDebugInfo()

            info.services.associate { it.cls.fqn to Reported(it.type, it.kind, it.overridden) } shouldBe mapOf(
                ToolsSingle::class.java.name to Reported.StaticSingleton,
                KontainerBlueprint::class.java.name to Reported.StaticSingleton,
                ToolsPromoted::class.java.name to Reported.SemiDynamicSingleton,
                ToolsDyn::class.java.name to Reported.DynamicSingleton,
                ToolsOverridden::class.java.name to Reported.OverriddenDynamicSingleton,
                Kontainer::class.java.name to Reported.DynamicSingleton,
                InjectionContext::class.java.name to Reported.DynamicSingleton,
                ToolsProto::class.java.name to Reported.StaticPrototype,
                ToolsForms::class.java.name to Reported.SemiDynamicPrototype,
            )
        }

        "Debug info: services are listed as singletons, semi dynamics, dynamics, prototypes" {

            val info = createAndUse().tools.getDebugInfo()

            info.services.map { it.cls.fqn } shouldBe listOf(
                ToolsSingle::class,
                KontainerBlueprint::class,
                ToolsPromoted::class,
                ToolsDyn::class,
                ToolsOverridden::class,
                Kontainer::class,
                InjectionContext::class,
                ToolsProto::class,
                ToolsForms::class,
            ).map { it.java.name }
        }

        "Debug info: the declared type and kind, which for a SemiDynamic is a static singleton" {

            val info = createAndUse().tools.getDebugInfo()

            assertSoftly {
                info.service(ToolsSingle::class).definition.let { Reported(it.type, it.kind) } shouldBe Reported.StaticSingleton
                info.service(ToolsPromoted::class).definition.let { Reported(it.type, it.kind) } shouldBe Reported.StaticSingleton
                info.service(ToolsDyn::class).definition.let { Reported(it.type, it.kind) } shouldBe Reported.DynamicSingleton
                info.service(ToolsOverridden::class).definition.let { Reported(it.type, it.kind) } shouldBe Reported.DynamicSingleton
                info.service(ToolsProto::class).definition.let { Reported(it.type, it.kind) } shouldBe Reported.StaticPrototype
                info.service(Kontainer::class).definition.let { Reported(it.type, it.kind) } shouldBe Reported.DynamicSingleton
                info.service(InjectionContext::class).definition.let { Reported(it.type, it.kind) } shouldBe Reported.DynamicSingleton
                info.service(KontainerBlueprint::class).definition.let { Reported(it.type, it.kind) } shouldBe Reported.StaticSingleton
            }
        }

        "Debug info: an override creates the given class and overwrites the definition from the blueprint" {

            val info = createAndUse().tools.getDebugInfo()
            val overridden = info.service(ToolsOverridden::class)

            assertSoftly {
                overridden.definition.creates.fqn shouldBe ToolsOverride::class.java.name
                overridden.definition.injects shouldBe emptyList()

                val overwrites = overridden.definition.overwrites.shouldNotBeNull()
                overwrites.creates.fqn shouldBe ToolsOverridden::class.java.name
                Reported(overwrites.type, overwrites.kind) shouldBe Reported.DynamicSingleton
                overwrites.overwrites.shouldBeNull()

                info.service(ToolsDyn::class).definition.overwrites.shouldBeNull()
            }
        }

        "Debug info: the instances each provider created in this kontainer" {

            val info = createAndUse().tools.getDebugInfo()

            assertSoftly {
                info.service(ToolsProto::class).instances shouldHaveSize 2
                info.service(ToolsDyn::class).instances shouldHaveSize 1
                info.service(ToolsPromoted::class).instances shouldHaveSize 1
                info.service(ToolsForms::class).instances shouldHaveSize 0

                withClue("an override reports its instance, created at create") {
                    info.service(ToolsOverridden::class).instances.map { it.cls.fqn } shouldBe
                            listOf(ToolsOverride::class.java.name)
                }
            }
        }

        "Debug info: the injected parameters, their provision type and the classes they resolve to" {

            val info = createAndUse().tools.getDebugInfo()

            val injects = info.service(ToolsForms::class).definition.injects
                .map { Triple(it.name, it.provisionType, it.classes.map { c -> c.fqn }.toSet()) }

            val candidates = setOf(ToolsSingle::class.java.name, ToolsDyn::class.java.name)

            injects shouldBe listOf(
                Triple("plain", ProvisionType.Direct, setOf(ToolsSingle::class.java.name)),
                Triple("nullable", ProvisionType.Direct, setOf(ToolsProto::class.java.name)),
                Triple("lazy", ProvisionType.Lazy, setOf(ToolsSingle::class.java.name)),
                Triple("list", ProvisionType.Direct, candidates),
                Triple("lazyList", ProvisionType.Lazy, candidates),
                Triple("lookup", ProvisionType.Lazy, candidates),
                Triple("context", ProvisionType.Direct, setOf(InjectionContext::class.java.name)),
            )
        }

        "Dump: a header and one row per service with its type" {

            val dump = createAndUse().tools.dumpKontainer()
            val lines = dump.lines()

            fun rowOf(cls: KClass<*>): String = lines.first { it.startsWith(cls.getName() + " ") }

            fun String.column(idx: Int) = split("|")[idx].trim()

            fun String.columns() = listOf(column(1), column(2))

            assertSoftly {
                dump shouldStartWith "Kontainer dump:\n\n"
                lines[2].split("|").map { it.trim() } shouldBe listOf("Service ID", "Type", "Kind", "Instances")

                rowOf(ToolsSingle::class).columns() shouldBe listOf("Singleton", "Static")
                rowOf(ToolsPromoted::class).columns() shouldBe listOf("Singleton", "SemiDynamic (ToolsDyn)")
                rowOf(ToolsDyn::class).columns() shouldBe listOf("Singleton", "Dynamic")
                rowOf(ToolsOverridden::class).columns() shouldBe listOf("Singleton", "Dynamic (overridden)")
                rowOf(ToolsProto::class).columns() shouldBe listOf("Prototype", "Static")
                rowOf(Kontainer::class).columns() shouldBe listOf("Singleton", "Dynamic")
                rowOf(InjectionContext::class).columns() shouldBe listOf("Singleton", "Dynamic")
                rowOf(KontainerBlueprint::class).columns() shouldBe listOf("Singleton", "Static")

                rowOf(ToolsOverridden::class).column(3) shouldEndWith ": " + ToolsOverride::class.getName()

                lines.drop(3).filter { it.contains(" | ") } shouldHaveSize 9
            }
        }
    }
}
