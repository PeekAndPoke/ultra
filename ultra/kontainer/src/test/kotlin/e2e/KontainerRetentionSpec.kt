package io.peekandpoke.ultra.kontainer.e2e

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.kontainer.KontainerBlueprint
import io.peekandpoke.ultra.kontainer.kontainer
import java.lang.ref.WeakReference

class RetainedDyn(val payload: Any?)

/**
 * Pins that a kontainer, once dropped, leaves neither its override lambdas nor their values reachable from the
 * blueprint (ULTRA-00337, ULTRA-00333).
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

    "An override lambda that was never run is not kept alive by the blueprint once its kontainer is gone" {

        val blueprint = kontainer { dynamic(RetainedDyn::class) { RetainedDyn(null) } }

        val marker = createWithUnusedOverride(blueprint)

        withClue("the marker captured by the override lambda must be collectable") {
            marker.clearsWithinBoundedGc() shouldBe true
        }
    }

    "An override that was used leaves neither its lambda's capture nor its value reachable once its kontainer is gone" {

        val blueprint = kontainer { dynamic(RetainedDyn::class) { RetainedDyn(null) } }

        val (captured, value) = createWithUsedOverride(blueprint)

        withClue("the marker captured by the override lambda must be collectable") {
            captured.clearsWithinBoundedGc() shouldBe true
        }

        withClue("the value the override produced must be collectable") {
            value.clearsWithinBoundedGc() shouldBe true
        }
    }
})

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
