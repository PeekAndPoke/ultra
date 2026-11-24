package io.peekandpoke.ultra.kontainer.e2e

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import io.peekandpoke.ultra.common.Lookup
import io.peekandpoke.ultra.kontainer.InjectionContext
import io.peekandpoke.ultra.kontainer.Kontainer
import io.peekandpoke.ultra.kontainer.KontainerBlueprint
import io.peekandpoke.ultra.kontainer.KontainerBuilder
import io.peekandpoke.ultra.kontainer.ServiceKind
import io.peekandpoke.ultra.kontainer.kontainer

abstract class GkBase
class GkSingle : GkBase()
class GkProto : GkBase()
class GkDyn : GkBase()
class GkPromoted(val dyn: GkDyn)

/** A global singleton with every injection form. */
class GkGlobal(
    val direct: GkSingle,
    val proto: GkProto,
    val list: List<GkBase>,
    val lazy: Lazy<GkSingle>,
    val lazyList: Lazy<List<GkBase>>,
    val lookup: Lookup<GkBase>,
)

/** Every injection form, of static services only, so it is Static itself. */
class GkStaticForms(
    val direct: GkSingle,
    val nullable: GkProto?,
    val list: List<GkSingle>,
    val lazy: Lazy<GkProto>,
    val lazyList: Lazy<List<GkSingle>>,
    val lookup: Lookup<GkSingle>,
    val blueprint: KontainerBlueprint,
)

/** Reaches a dynamic through each form in turn, so each is promoted. */
class GkLazyDyn(val lazy: Lazy<GkDyn>)
class GkLookupDyn(val lookup: Lookup<GkBase>)
class GkContext(val context: InjectionContext)
class GkKontainer(val kontainer: Kontainer)
class GkChained(val promoted: GkPromoted)

/**
 * Pins the blueprint-wide kontainer (ULTRA-00329): every global singleton is created in it, with all its parameters,
 * and resolves the same services there as in any request's kontainer.
 */
class GlobalKontainerSpec : StringSpec({

    /** Without [GkDyn], so every [GkBase] candidate is static and [GkGlobal] is a global singleton. */
    fun staticBlueprint(config: KontainerBlueprint.Config = KontainerBlueprint.Config.default) = KontainerBuilder {
        singleton(GkSingle::class)
        prototype(GkProto::class)
        singleton(GkGlobal::class)
    }.build(config)

    "The blueprint-wide kontainer is created on the first creation of a global singleton, once, and is not tracked" {

        val blueprint = KontainerBuilder {
            singleton(GkSingle::class)
            dynamic(GkDyn::class)
            singleton(GkPromoted::class)
            prototype(GkProto::class)
        }.build(KontainerBlueprint.Config(trackKontainers = true))

        val subject = blueprint.create()

        assertSoftly {
            subject.get(GkDyn::class)
            subject.get(GkPromoted::class)
            subject.get(GkProto::class)

            withClue("nothing global was created yet") {
                blueprint.globalKontainerLazy.isInitialized() shouldBe false
            }

            subject.get(GkSingle::class)

            blueprint.globalKontainerLazy.isInitialized() shouldBe true
            blueprint.globalKontainer shouldBeSameInstanceAs blueprint.globalKontainer

            withClue("the tracker counts request kontainers for leak detection; this one lives as long as the blueprint") {
                blueprint.tracker.getAlive().keys shouldContain subject
                blueprint.tracker.getAlive().keys shouldNotContain blueprint.globalKontainer
            }
        }
    }

    "A global singleton is created in the blueprint-wide kontainer, with all its parameters" {

        val blueprint = staticBlueprint()

        val one = blueprint.create()
        val two = blueprint.create()

        val global = one.get(GkGlobal::class)

        assertSoftly {
            one.reportedOf(GkGlobal::class) shouldBe Reported.StaticSingleton

            withClue("the prototype injected directly and the one in the list are created by the blueprint-wide kontainer") {
                one.getProvider(GkProto::class).created.count shouldBe 0L
                blueprint.globalKontainer.getProvider(GkProto::class).created.count shouldBe 2L
            }

            repeat(3) { two.get(GkGlobal::class).lookup.get(GkProto::class) }
            global.lazyList.value

            withClue("the Lookup and the Lazy<List> create there too, whichever kontainer the singleton is used through") {
                one.getProvider(GkProto::class).created.count shouldBe 0L
                two.getProvider(GkProto::class).created.count shouldBe 0L
                blueprint.globalKontainer.getProvider(GkProto::class).created.count shouldBe 6L
            }
        }
    }

    "Through a global singleton, a static singleton is the request's instance, a static prototype a new one of its class" {

        val blueprint = staticBlueprint()
        val subject = blueprint.create()
        val global = subject.get(GkGlobal::class)
        val single = subject.get(GkSingle::class)

        assertSoftly {
            global.direct shouldBeSameInstanceAs single
            global.lazy.value shouldBeSameInstanceAs single
            global.lookup.get(GkSingle::class) shouldBeSameInstanceAs single
            global.list.filterIsInstance<GkSingle>().single() shouldBeSameInstanceAs single
            global.lazyList.value.filterIsInstance<GkSingle>().single() shouldBeSameInstanceAs single

            val protos = listOf(
                global.proto,
                global.lookup.get(GkProto::class),
                global.list.filterIsInstance<GkProto>().single(),
                global.lazyList.value.filterIsInstance<GkProto>().single(),
                subject.get(GkProto::class),
            )

            protos.forEach { it.shouldBeInstanceOf<GkProto>() }
            protos.toSet().size shouldBe protos.size
            global.lookup.get(GkProto::class) shouldNotBeSameInstanceAs global.lookup.get(GkProto::class)
        }
    }

    "Everything a Static service reaches, through any injection form, is Static as well" {

        val subject = kontainer {
            singleton(GkSingle::class)
            prototype(GkProto::class)
            dynamic(GkDyn::class)
            singleton(GkPromoted::class)
            singleton(GkGlobal::class)
            singleton(GkStaticForms::class)
            prototype(GkLazyDyn::class)
            singleton(GkLookupDyn::class)
            singleton(GkContext::class)
            singleton(GkKontainer::class)
            singleton(GkChained::class)
        }.create()

        val services = subject.tools.getDebugInfo().services.associateBy { it.cls.fqn }

        fun reachedBy(fqn: String): Set<String> {
            val seen = mutableSetOf<String>()
            val todo = ArrayDeque(listOf(fqn))

            while (todo.isNotEmpty()) {
                services.getValue(todo.removeFirst()).definition.injects
                    .flatMap { param -> param.classes.map { it.fqn } }
                    .filter { seen.add(it) }
                    .forEach { todo.add(it) }
            }

            return seen
        }

        val statics = services.values.filter { it.kind == ServiceKind.Static }.map { it.cls.fqn }

        assertSoftly {
            withClue("the guard is not vacuous: the all-forms service is Static and reaches services") {
                statics shouldContain GkStaticForms::class.java.name
                reachedBy(GkStaticForms::class.java.name).shouldNotBeEmpty()
            }

            withClue("each way of reaching a dynamic promotes") {
                listOf(GkPromoted::class, GkLazyDyn::class, GkLookupDyn::class, GkContext::class, GkKontainer::class, GkChained::class)
                    .forEach { statics shouldNotContain it.java.name }
            }

            statics.forEach { static ->
                reachedBy(static).forEach { reached ->
                    withClue("$static reaches $reached") {
                        services.getValue(reached).kind shouldBe ServiceKind.Static
                    }
                }
            }
        }
    }
})
