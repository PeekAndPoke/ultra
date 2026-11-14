package io.peekandpoke.ultra.kontainer.e2e.characterization

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.kontainer.InjectionContext
import io.peekandpoke.ultra.kontainer.Kontainer
import io.peekandpoke.ultra.kontainer.KontainerBlueprint
import io.peekandpoke.ultra.kontainer.KontainerBuilder
import io.peekandpoke.ultra.kontainer.KontainerInconsistent
import io.peekandpoke.ultra.kontainer.getName
import io.peekandpoke.ultra.kontainer.kontainer
import io.peekandpoke.ultra.kontainer.module
import kotlin.reflect.KClass

/**
 * Pins that the three built-in services, `Kontainer`, `InjectionContext` and `KontainerBlueprint`, can neither be
 * registered nor overridden (ULTRA-00336). Before, every registration was accepted silently (commit `a4977d16`).
 */
class BuiltInServicesCharacterizationSpec : StringSpec({

    class Form(val name: String, val register: KontainerBuilder.() -> Unit)

    fun forms(cls: KClass<*>): List<Form> {

        @Suppress("UNCHECKED_CAST")
        val any = cls as KClass<Any>

        val byFactory: KontainerBuilder.(KontainerBuilder.ServiceBuilder) -> Unit = when (cls) {
            Kontainer::class -> { b -> b(Kontainer::class) { k: Kontainer -> k } }
            InjectionContext::class -> { b -> b(InjectionContext::class) { c: InjectionContext -> c } }
            else -> { b -> b(KontainerBlueprint::class) { bp: KontainerBlueprint -> bp } }
        }

        val instance: Any = when (cls) {
            Kontainer::class -> kontainer { }.create()
            InjectionContext::class -> InjectionContext.kontainerRoot
            else -> kontainer { }
        }

        val builders = listOf<Pair<String, KontainerBuilder.() -> KontainerBuilder.ServiceBuilder>>(
            "singleton" to { singleton },
            "prototype" to { prototype },
            "dynamic" to { dynamic },
            "dynamicPrototype" to { dynamicPrototype },
        )

        return builders.flatMap { (name, builder) ->
            listOf(
                Form("$name by class") { builder()(any) },
                Form("$name by factory") { byFactory(this, builder()) },
            )
        } + listOf(
            Form("instance(value)") { instance(instance) },
            Form("instance(class, value)") { instance(any, instance) },
        )
    }

    fun registered(cls: KClass<*>) = "${cls.getName()} is managed by the kontainer itself and cannot be registered"

    fun overridden(cls: KClass<*>) = "${cls.getName()} is managed by the kontainer itself and cannot be overridden"

    listOf(Kontainer::class, InjectionContext::class, KontainerBlueprint::class).forEach { cls ->
        forms(cls).forEach { form ->

            "Registering ${cls.simpleName} by ${form.name} in a builder is refused at the defining call" {
                var reachedNextLine = false

                val error = shouldThrow<KontainerInconsistent> {
                    kontainer {
                        form.register(this)
                        reachedNextLine = true
                    }
                }

                assertSoftly {
                    error.message shouldBe registered(cls)
                    reachedNextLine shouldBe false
                }
            }

            "Registering ${cls.simpleName} by ${form.name} in a module is refused" {
                val registering = module { form.register(this) }

                shouldThrow<KontainerInconsistent> {
                    kontainer { module(registering) }
                }.message shouldBe registered(cls)
            }

            "Registering ${cls.simpleName} by ${form.name} in extend is refused" {
                val base = kontainer { }

                shouldThrow<KontainerInconsistent> {
                    base.extend { form.register(this) }
                }.message shouldBe registered(cls)
            }
        }
    }

    "An override of InjectionContext at create is refused" {

        shouldThrow<KontainerInconsistent> {
            kontainer { }.create { with { InjectionContext.kontainerRoot } }
        }.message shouldBe overridden(InjectionContext::class)
    }

    "An override of KontainerBlueprint at create is refused with the same clear message" {

        shouldThrow<KontainerInconsistent> {
            kontainer { }.create { with { kontainer { } } }
        }.message shouldBe overridden(KontainerBlueprint::class)
    }

    "An override of Kontainer at clone is refused" {

        val blueprint = kontainer { }
        val foreign = blueprint.create()

        shouldThrow<KontainerInconsistent> {
            blueprint.create().clone { with<Kontainer> { foreign } }
        }.message shouldBe overridden(Kontainer::class)
    }

    "An override of InjectionContext at clone is refused" {

        shouldThrow<KontainerInconsistent> {
            kontainer { }.create().clone { with { InjectionContext.kontainerRoot } }
        }.message shouldBe overridden(InjectionContext::class)
    }

    "An override of KontainerBlueprint at clone is refused" {

        shouldThrow<KontainerInconsistent> {
            kontainer { }.create().clone { with { kontainer { } } }
        }.message shouldBe overridden(KontainerBlueprint::class)
    }

    "A built-in next to an unexpected override: the built-in is named, before the dynamics check" {

        class Single

        shouldThrow<KontainerInconsistent> {
            kontainer { singleton(Single::class) }.create {
                with { Single() }
                with { InjectionContext.kontainerRoot }
            }
        }.message shouldBe overridden(InjectionContext::class)
    }
})
