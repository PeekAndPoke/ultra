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
import io.peekandpoke.ultra.kontainer.InjectionType
import io.peekandpoke.ultra.kontainer.Kontainer
import io.peekandpoke.ultra.kontainer.KontainerBlueprint
import io.peekandpoke.ultra.kontainer.ServiceProvider.Type
import io.peekandpoke.ultra.kontainer.domain.DebugInfo
import io.peekandpoke.ultra.kontainer.domain.DebugInfo.ParamInfo.ProvisionType
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
 * Pins what [Kontainer.tools] report for one blueprint holding one service of each [Type].
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
                Type.entries.map { it.name } shouldBe
                        listOf("Singleton", "Prototype", "SemiDynamic", "Dynamic", "DynamicOverride")
                InjectionType.entries.map { it.name } shouldBe listOf("Singleton", "Prototype", "Dynamic")
                ProvisionType.entries.map { it.name } shouldBe listOf("Direct", "Lazy")
            }
        }

        "Debug info: the type of every service, the built-in ones included" {

            val info = createAndUse().tools.getDebugInfo()

            info.services.associate { it.cls.fqn to it.type } shouldBe mapOf(
                ToolsSingle::class.java.name to Type.Singleton,
                KontainerBlueprint::class.java.name to Type.Singleton,
                ToolsPromoted::class.java.name to Type.SemiDynamic,
                ToolsDyn::class.java.name to Type.Dynamic,
                ToolsOverridden::class.java.name to Type.DynamicOverride,
                Kontainer::class.java.name to Type.Dynamic,
                InjectionContext::class.java.name to Type.Dynamic,
                ToolsProto::class.java.name to Type.Prototype,
                ToolsForms::class.java.name to Type.Prototype,
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

        "Debug info: the declared injection type, which for a SemiDynamic is Singleton" {

            val info = createAndUse().tools.getDebugInfo()

            assertSoftly {
                info.service(ToolsSingle::class).definition.injectionType shouldBe InjectionType.Singleton
                info.service(ToolsPromoted::class).definition.injectionType shouldBe InjectionType.Singleton
                info.service(ToolsDyn::class).definition.injectionType shouldBe InjectionType.Dynamic
                info.service(ToolsOverridden::class).definition.injectionType shouldBe InjectionType.Dynamic
                info.service(ToolsProto::class).definition.injectionType shouldBe InjectionType.Prototype
                info.service(Kontainer::class).definition.injectionType shouldBe InjectionType.Dynamic
                info.service(InjectionContext::class).definition.injectionType shouldBe InjectionType.Dynamic
                info.service(KontainerBlueprint::class).definition.injectionType shouldBe InjectionType.Singleton
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
                overwrites.injectionType shouldBe InjectionType.Dynamic
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

            assertSoftly {
                dump shouldStartWith "Kontainer dump:\n\n"
                lines[2].split("|").map { it.trim() } shouldBe listOf("Service ID", "Type", "Instances")

                rowOf(ToolsSingle::class).column(1) shouldBe "Singleton"
                rowOf(ToolsPromoted::class).column(1) shouldBe "SemiDynamic"
                rowOf(ToolsDyn::class).column(1) shouldBe "Dynamic"
                rowOf(ToolsOverridden::class).column(1) shouldBe "DynamicOverride"
                rowOf(ToolsProto::class).column(1) shouldBe "Prototype"
                rowOf(Kontainer::class).column(1) shouldBe "Dynamic"
                rowOf(InjectionContext::class).column(1) shouldBe "Dynamic"
                rowOf(KontainerBlueprint::class).column(1) shouldBe "Singleton"

                rowOf(ToolsOverridden::class).column(2) shouldEndWith ": " + ToolsOverride::class.getName()

                lines.drop(3).filter { it.contains(" | ") } shouldHaveSize 9
            }
        }
    }
}
