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
import io.peekandpoke.ultra.kontainer.LazyServiceLookup
import io.peekandpoke.ultra.kontainer.ServiceKind
import io.peekandpoke.ultra.kontainer.ServiceType
import io.peekandpoke.ultra.kontainer.kontainer
import java.util.Collections
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.reflect.KClass

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
class GkLazyDynSingle(val lazy: Lazy<GkDyn>)

/** A static prototype that counts its births, injected by each of the [GkRace] singletons. */
class GkRaceProto {
    companion object {
        val births = AtomicInteger()
    }

    init {
        births.incrementAndGet()
    }
}

/** Eight distinct static singletons, so eight threads can each create a different one at the same time. */
abstract class GkRace(val proto: GkRaceProto)
class GkRace0(proto: GkRaceProto) : GkRace(proto)
class GkRace1(proto: GkRaceProto) : GkRace(proto)
class GkRace2(proto: GkRaceProto) : GkRace(proto)
class GkRace3(proto: GkRaceProto) : GkRace(proto)
class GkRace4(proto: GkRaceProto) : GkRace(proto)
class GkRace5(proto: GkRaceProto) : GkRace(proto)
class GkRace6(proto: GkRaceProto) : GkRace(proto)
class GkRace7(proto: GkRaceProto) : GkRace(proto)

private val gkRaces: List<KClass<out GkRace>> = listOf(
    GkRace0::class, GkRace1::class, GkRace2::class, GkRace3::class,
    GkRace4::class, GkRace5::class, GkRace6::class, GkRace7::class,
)

/** Reads every [Lazy] and gets every service of every [LazyServiceLookup] held in a field of [service]. */
private fun resolveLazies(service: Any) {
    service.javaClass.declaredFields.forEach { field ->
        field.isAccessible = true

        when (val value = field.get(service)) {
            is Lazy<*> -> value.value
            is LazyServiceLookup<*> -> {
                @Suppress("UNCHECKED_CAST")
                val lookup = value as LazyServiceLookup<Any>
                lookup.classes.forEach { lookup.get(it) }
            }
        }
    }
}

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

    "Concurrent first creations of different global singletons, each from its own kontainer, share one blueprint-wide kontainer" {

        repeat(100) {
            GkRaceProto.births.set(0)

            val blueprint = kontainer {
                prototype(GkRaceProto::class)
                gkRaces.forEach { singleton(it) }
            }

            val kontainers = gkRaces.map { blueprint.create() }
            val barrier = CyclicBarrier(gkRaces.size)
            val errors = Collections.synchronizedList(mutableListOf<Throwable>())

            gkRaces.indices
                .map { idx ->
                    thread {
                        try {
                            barrier.await()
                            kontainers[idx].get(gkRaces[idx])
                        } catch (e: Throwable) {
                            errors.add(e)
                        }
                    }
                }
                .forEach { it.join() }

            assertSoftly {
                errors shouldBe emptyList()

                gkRaces.forEach { cls ->
                    kontainers.first().getProvider(cls).created.count shouldBe 1L
                }

                withClue("every prototype the singletons injected was created by the one blueprint-wide kontainer") {
                    GkRaceProto.births.get() shouldBe gkRaces.size
                    blueprint.globalKontainer.getProvider(GkRaceProto::class).created.count shouldBe gkRaces.size.toLong()
                }
            }
        }
    }

    "Everything a Static service reaches, through any injection form, is Static as well" {

        val blueprint = kontainer {
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
            singleton(GkLazyDynSingle::class)
        }

        val subject = blueprint.create()

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
                listOf(
                    GkPromoted::class, GkLazyDyn::class, GkLookupDyn::class, GkContext::class, GkKontainer::class,
                    GkChained::class, GkLazyDynSingle::class,
                ).forEach { statics shouldNotContain it.java.name }
            }

            statics.forEach { static ->
                reachedBy(static).forEach { reached ->
                    withClue("$static reaches $reached") {
                        services.getValue(reached).kind shouldBe ServiceKind.Static
                    }
                }
            }
        }

        // At runtime, independent of the announcements the kinds are computed from: create every Static singleton and
        // resolve all its lazies; the blueprint-wide kontainer must never have been asked for anything else.
        statics.map { Class.forName(it).kotlin }
            .filter { subject.getProvider(it).type == ServiceType.Singleton }
            .forEach { resolveLazies(subject.get(it)) }

        val factory = blueprint.globalKontainer.getServiceProviderFactory()

        withClue("the runtime check is not vacuous: the parameters of the static singletons were resolved there") {
            factory.isProviderCreated(GkSingle::class) shouldBe true
            factory.isProviderCreated(GkProto::class) shouldBe true
        }

        services.values.filter { it.kind != ServiceKind.Static }.forEach { service ->
            withClue("${service.cls.fqn} is ${service.kind}, the blueprint-wide kontainer must not provide it") {
                factory.isProviderCreated(Class.forName(service.cls.fqn).kotlin) shouldBe false
            }
        }
    }
})
