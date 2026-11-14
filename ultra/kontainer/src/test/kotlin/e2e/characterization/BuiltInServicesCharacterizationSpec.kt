package io.peekandpoke.ultra.kontainer.e2e.characterization

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.peekandpoke.ultra.kontainer.InjectionContext
import io.peekandpoke.ultra.kontainer.Kontainer
import io.peekandpoke.ultra.kontainer.KontainerBlueprint
import io.peekandpoke.ultra.kontainer.KontainerBuilder
import io.peekandpoke.ultra.kontainer.KontainerInconsistent
import io.peekandpoke.ultra.kontainer.ServiceNotFound
import io.peekandpoke.ultra.kontainer.e2e.Reported
import io.peekandpoke.ultra.kontainer.e2e.reportedOf
import io.peekandpoke.ultra.kontainer.getName
import io.peekandpoke.ultra.kontainer.kontainer
import io.peekandpoke.ultra.kontainer.module
import kotlin.reflect.KClass

/**
 * Pins what registering or overriding one of the three built-in services does: `Kontainer`, `InjectionContext` and
 * `KontainerBlueprint` (ULTRA-00336).
 */
class BuiltInServicesCharacterizationSpec : StringSpec({

    /** A registration form; what today's kontainer reports for the built-in afterwards. */
    class Form(val name: String, val reportedToday: (KClass<*>) -> Reported, val register: KontainerBuilder.() -> Unit)

    fun builtInDefault(cls: KClass<*>) = when (cls) {
        KontainerBlueprint::class -> Reported.StaticSingleton
        else -> Reported.DynamicSingleton
    }

    fun forms(cls: KClass<*>): List<Form> {

        @Suppress("UNCHECKED_CAST")
        val any = cls as KClass<Any>

        val byFactory: Map<String, KontainerBuilder.(KontainerBuilder.ServiceBuilder) -> Unit> = when (cls) {
            Kontainer::class -> mapOf("factory" to { b -> b(Kontainer::class) { k: Kontainer -> k } })
            InjectionContext::class -> mapOf("factory" to { b -> b(InjectionContext::class) { c: InjectionContext -> c } })
            else -> mapOf("factory" to { b -> b(KontainerBlueprint::class) { bp: KontainerBlueprint -> bp } })
        }

        val instance: Any = when (cls) {
            Kontainer::class -> kontainer { }.create()
            InjectionContext::class -> InjectionContext.kontainerRoot
            else -> kontainer { }
        }

        val builders = listOf<Triple<String, (KClass<*>) -> Reported, KontainerBuilder.() -> KontainerBuilder.ServiceBuilder>>(
            Triple("singleton", { c -> builtInDefault(c) }, { singleton }),
            Triple("prototype", { _ -> Reported.StaticPrototype }, { prototype }),
            Triple("dynamic", { _ -> Reported.DynamicSingleton }, { dynamic }),
            Triple("dynamicPrototype", { _ -> Reported.DynamicPrototype }, { dynamicPrototype }),
        )

        return builders.flatMap { (name, reported, builder) ->
            listOf(
                Form("$name by class", reported) { builder()(any) },
                Form("$name by factory", reported) { byFactory.getValue("factory")(this, builder()) },
            )
        } + listOf(
            Form("instance(value)", { c -> builtInDefault(c) }) { instance(instance) },
            Form("instance(class, value)", { c -> builtInDefault(c) }) { instance(any, instance) },
        )
    }

    /** Checks that the built-in still provides its own value in [subject]. */
    fun builtInWins(cls: KClass<*>, subject: Kontainer, blueprint: KontainerBlueprint) {
        when (cls) {
            Kontainer::class -> subject.get(Kontainer::class) shouldBeSameInstanceAs subject
            KontainerBlueprint::class -> subject.get(KontainerBlueprint::class) shouldBeSameInstanceAs blueprint
            else -> subject.get(InjectionContext::class) shouldBeSameInstanceAs InjectionContext.kontainerRoot
        }
    }

    listOf(Kontainer::class, InjectionContext::class, KontainerBlueprint::class).forEach { cls ->
        forms(cls).forEach { form ->

            "CURRENT BEHAVIOUR: registering ${cls.simpleName} by ${form.name} in a builder is accepted, the built-in wins" {
                val blueprint = kontainer { form.register(this) }
                val subject = blueprint.create()

                assertSoftly {
                    builtInWins(cls, subject, blueprint)
                    subject.reportedOf(cls) shouldBe form.reportedToday(cls)
                }
            }

            "CURRENT BEHAVIOUR: registering ${cls.simpleName} by ${form.name} in a module is accepted, the built-in wins" {
                val registering = module { form.register(this) }
                val blueprint = kontainer { module(registering) }
                val subject = blueprint.create()

                assertSoftly {
                    builtInWins(cls, subject, blueprint)
                    subject.reportedOf(cls) shouldBe form.reportedToday(cls)
                }
            }

            "CURRENT BEHAVIOUR: registering ${cls.simpleName} by ${form.name} in extend is accepted, the built-in wins" {
                val blueprint = kontainer { }.extend { form.register(this) }
                val subject = blueprint.create()

                assertSoftly {
                    builtInWins(cls, subject, blueprint)
                    subject.reportedOf(cls) shouldBe form.reportedToday(cls)
                }
            }
        }
    }

    "CURRENT BEHAVIOUR: an override of InjectionContext at create is accepted" {

        class UsesContext(val context: InjectionContext)

        val blueprint = kontainer { prototype(UsesContext::class) }

        val subject = blueprint.create { with { InjectionContext.kontainerRoot } }

        subject.reportedOf(InjectionContext::class) shouldBe Reported.OverriddenDynamicSingleton
    }

    "CURRENT BEHAVIOUR: an override of KontainerBlueprint at create is refused as unexpected" {

        val blueprint = kontainer { }

        shouldThrow<KontainerInconsistent> {
            blueprint.create { with { kontainer { } } }
        }.message shouldBe "Unexpected dynamics were provided: ${KontainerBlueprint::class.getName()}"
    }

    "CURRENT BEHAVIOUR: an override of Kontainer at clone is accepted" {

        val blueprint = kontainer { }
        val foreign = blueprint.create()
        val clone = blueprint.create().clone { with<Kontainer> { foreign } }

        clone.get(Kontainer::class) shouldBeSameInstanceAs foreign
    }

    "CURRENT BEHAVIOUR: an override of InjectionContext at clone is accepted" {

        val clone = kontainer { }.create().clone { with { InjectionContext.kontainerRoot } }

        clone.reportedOf(InjectionContext::class) shouldBe Reported.OverriddenDynamicSingleton
    }

    "CURRENT BEHAVIOUR: an override of KontainerBlueprint at clone throws ServiceNotFound" {

        shouldThrow<ServiceNotFound> {
            kontainer { }.create().clone { with { kontainer { } } }
        }
    }
})
