package io.peekandpoke.ultra.kontainer.e2e

import io.kotest.assertions.assertSoftly
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.kontainer.Kontainer
import io.peekandpoke.ultra.kontainer.kontainer

open class OrderBase(val name: String)
class OrderDerived(name: String) : OrderBase(name)

/**
 * Pins that the override given last wins when several hit one dynamic service, in one create and across clone
 * (ULTRA-00355). Before, a class given again kept its first place and could lose to an override given before it.
 */
class OverrideOrderSpec : StringSpec({

    val blueprint = kontainer { dynamic(OrderBase::class) { OrderBase("default") } }

    /** The name of the instance that serves [OrderBase]. */
    fun Kontainer.winner() = get(OrderBase::class).name

    // Changed by ULTRA-00355 ////////////////////////////////////////////////////////////////////////////////////////

    "In one create, a class given again moves to the end: Base A, Derived B, Base C yields C" {

        val log = mutableListOf<String>()

        val subject = blueprint.create {
            with(OrderBase::class) { log.add("A"); OrderBase("A") }
            with(OrderDerived::class) { log.add("B"); OrderDerived("B") }
            with(OrderBase::class) { log.add("C"); OrderBase("C") }
        }

        assertSoftly {
            subject.winner() shouldBe "C"
            log shouldBe listOf("B", "C")
        }
    }

    "At clone, the clone's override wins: create Base A, Derived B, then clone Base C yields C" {

        val log = mutableListOf<String>()

        val original = blueprint.create {
            with(OrderBase::class) { log.add("A"); OrderBase("A") }
            with(OrderDerived::class) { log.add("B"); OrderDerived("B") }
        }

        log.clear()
        val clone = original.clone { with(OrderBase::class) { log.add("C"); OrderBase("C") } }

        assertSoftly {
            clone.winner() shouldBe "C"
            log shouldBe listOf("B", "C")
            original.winner() shouldBe "B"
        }
    }

    "A clone of a clone: the override given last, at the second clone, wins" {

        val original = blueprint.create {
            with(OrderBase::class) { OrderBase("A") }
            with(OrderDerived::class) { OrderDerived("B") }
        }

        val first = original.clone { with(OrderBase::class) { OrderBase("C") } }
        val second = first.clone { with(OrderDerived::class) { OrderDerived("D") } }
        val third = second.clone { with(OrderBase::class) { OrderBase("E") } }

        assertSoftly {
            listOf(original, first, second, third).map { it.winner() } shouldBe listOf("B", "C", "D", "E")
        }
    }

    // Unchanged //////////////////////////////////////////////////////////////////////////////////////////////////////

    "No override uses the default, one override is used" {

        assertSoftly {
            blueprint.create().winner() shouldBe "default"
            blueprint.create { with(OrderBase::class) { OrderBase("A") } }.winner() shouldBe "A"
        }
    }

    "Two classes in order: the later one wins, in either order" {

        val baseFirst = blueprint.create {
            with(OrderBase::class) { OrderBase("A") }
            with(OrderDerived::class) { OrderDerived("B") }
        }

        val derivedFirst = blueprint.create {
            with(OrderDerived::class) { OrderDerived("B") }
            with(OrderBase::class) { OrderBase("A") }
        }

        assertSoftly {
            baseFirst.winner() shouldBe "B"
            derivedFirst.winner() shouldBe "A"
        }
    }

    "The same class given twice in one create: the second is used, the first never runs" {

        val log = mutableListOf<String>()

        val subject = blueprint.create {
            with(OrderBase::class) { log.add("A"); OrderBase("A") }
            with(OrderBase::class) { log.add("C"); OrderBase("C") }
        }

        assertSoftly {
            subject.winner() shouldBe "C"
            log shouldBe listOf("C")
        }
    }

    "A clone overriding the same class as its parent uses the clone's override" {

        val original = blueprint.create { with(OrderBase::class) { OrderBase("A") } }
        val clone = original.clone { with(OrderBase::class) { OrderBase("C") } }

        assertSoftly {
            clone.winner() shouldBe "C"
            original.winner() shouldBe "A"
        }
    }

    "A clone without overrides keeps the parent's winner and runs the parent's lambdas in their order" {

        val log = mutableListOf<String>()

        val original = blueprint.create {
            with(OrderBase::class) { log.add("A"); OrderBase("A") }
            with(OrderDerived::class) { log.add("B"); OrderDerived("B") }
        }

        log.clear()
        val clone = original.clone()

        assertSoftly {
            clone.winner() shouldBe "B"
            log shouldBe listOf("A", "B")
        }
    }

    "A clone adding an override under a new class: the new one, given last, wins" {

        val original = blueprint.create { with(OrderBase::class) { OrderBase("A") } }
        val clone = original.clone { with(OrderDerived::class) { OrderDerived("B") } }

        clone.winner() shouldBe "B"
    }
})
