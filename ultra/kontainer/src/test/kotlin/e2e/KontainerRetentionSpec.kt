package io.peekandpoke.ultra.kontainer.e2e

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.common.Lookup
import io.peekandpoke.ultra.kontainer.Kontainer
import io.peekandpoke.ultra.kontainer.KontainerBlueprint
import io.peekandpoke.ultra.kontainer.kontainer
import java.lang.ref.Reference
import java.lang.ref.WeakReference
import kotlin.reflect.KClass

class RetainedDyn(val payload: Any?)

class RetainedProto
class RetainedSingle
class RetainedProtoWithLookup(val lookup: Lookup<RetainedProto>)

class GlobalWithLookup(val lookup: Lookup<RetainedProto>)
class GlobalWithLazy(val lazy: Lazy<RetainedSingle>)
class GlobalWithLazyList(val lazyList: Lazy<List<RetainedSingle>>)
class GlobalWithTransitiveLookup(val proto: RetainedProtoWithLookup)

/**
 * Pins that a kontainer, once dropped, leaves neither its override lambdas nor their values reachable from the
 * blueprint (ULTRA-00337), and that a global singleton created in it does not keep it alive (ULTRA-00329).
 */
class KontainerRetentionSpec : StringSpec({

    /** Collects garbage until [this] is cleared, bounded: false when it never clears. */
    fun WeakReference<*>.clearsWithinBoundedGc(): Boolean {
        repeat(100) {
            System.gc()
            if (get() == null) return true
            @Suppress("UNUSED_VARIABLE")
            val pressure = ByteArray(1_000_000)
            Thread.sleep(20)
        }
        return get() == null
    }

    "An override that was never requested is not kept alive by the blueprint once its kontainer is gone" {

        val blueprint = kontainer { dynamic(RetainedDyn::class) { RetainedDyn(null) } }

        val marker = createWithUnusedOverride(blueprint)

        withClue("the marker captured by the override lambda must be collectable") {
            marker.clearsWithinBoundedGc() shouldBe true
        }

        // The blueprint must stay reachable until here, or the test would prove nothing about it
        Reference.reachabilityFence(blueprint)
    }

    "An override that was requested leaves neither its lambda's capture nor its value reachable once its kontainer is gone" {

        val blueprint = kontainer { dynamic(RetainedDyn::class) { RetainedDyn(null) } }

        val (captured, value) = createWithUsedOverride(blueprint)

        withClue("the marker captured by the override lambda must be collectable") {
            captured.clearsWithinBoundedGc() shouldBe true
        }

        withClue("the value the override produced must be collectable") {
            value.clearsWithinBoundedGc() shouldBe true
        }

        // The blueprint must stay reachable until here, or the test would prove nothing about it
        Reference.reachabilityFence(blueprint)
    }

    /** The blueprint of the global singleton cases: every holder is Static, it reaches nothing dynamic. */
    fun globalsBlueprint() = kontainer {
        prototype(RetainedProto::class)
        singleton(RetainedSingle::class)
        prototype(RetainedProtoWithLookup::class)
        singleton(GlobalWithLookup::class)
        singleton(GlobalWithLazy::class)
        singleton(GlobalWithLazyList::class)
        singleton(GlobalWithTransitiveLookup::class)
    }

    listOf(
        GlobalWithLookup::class to "a Lookup",
        GlobalWithLazy::class to "an unread Lazy",
        GlobalWithLazyList::class to "an unread Lazy<List>",
        GlobalWithTransitiveLookup::class to "a static prototype that holds a Lookup",
    ).forEach { (cls, form) ->

        "A global singleton with $form does not keep the kontainer it was first requested from alive" {

            val blueprint = globalsBlueprint()

            val (holder, first) = getFromDroppedKontainer(blueprint, cls)

            withClue("the kontainer the global singleton was first requested from must be collectable") {
                first.clearsWithinBoundedGc() shouldBe true
            }

            // The holder and the blueprint must stay reachable until here, or the test would prove nothing
            Reference.reachabilityFence(holder)
            Reference.reachabilityFence(blueprint)
        }
    }
})

/** Gets [cls] from a new kontainer and keeps nothing of that kontainer but a weak reference. */
private fun getFromDroppedKontainer(blueprint: KontainerBlueprint, cls: KClass<*>): Pair<Any, WeakReference<Kontainer>> {
    val kontainer = blueprint.create()

    return kontainer.get(cls) to WeakReference(kontainer)
}

/** Creates a kontainer whose override captures a marker, never requests it, and keeps nothing but a weak reference. */
private fun createWithUnusedOverride(blueprint: KontainerBlueprint): WeakReference<Any> {
    val marker = Any()

    blueprint.create { with { RetainedDyn(marker) } }

    return WeakReference(marker)
}

/** Creates a kontainer whose override captures a marker, requests it once, and keeps nothing but weak references. */
private fun createWithUsedOverride(blueprint: KontainerBlueprint): Pair<WeakReference<Any>, WeakReference<Any>> {
    val marker = Any()

    val kontainer = blueprint.create { with { RetainedDyn(marker.hashCode()) } }
    val value = kontainer.get(RetainedDyn::class)

    return WeakReference(marker) to WeakReference<Any>(value)
}
