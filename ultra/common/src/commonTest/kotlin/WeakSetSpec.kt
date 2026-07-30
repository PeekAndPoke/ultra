package io.peekandpoke.ultra.common

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

/**
 * Membership behaviour of [WeakSet], on every platform.
 *
 * Uses strings and ints deliberately: JS routes non-object values to a strong fallback set that
 * matches structurally, which is what JVM and native do for everything. Object elements would
 * diverge — JS matches those by reference identity — so they are not asserted here.
 */
class WeakSetSpec : StringSpec({

    "contains is false for an element that was never added" {
        WeakSet<String>().contains("a") shouldBe false
    }

    "add makes an element a member" {
        val subject = WeakSet<String>()

        subject.add("a")

        subject.contains("a") shouldBe true
        subject.contains("b") shouldBe false
    }

    "adding the same element twice keeps it a single member" {
        val subject = WeakSet<String>()

        subject.add("a")
        subject.add("a")

        subject.contains("a") shouldBe true

        subject.remove("a")

        // one remove is enough — it was never stored twice
        subject.contains("a") shouldBe false
    }

    "remove drops an element" {
        val subject = WeakSet<Int>()

        subject.add(1)
        subject.add(2)

        subject.remove(1)

        subject.contains(1) shouldBe false
        subject.contains(2) shouldBe true
    }

    "removing an element that is not a member is a no-op" {
        val subject = WeakSet<Int>()

        subject.add(1)
        subject.remove(42)

        subject.contains(1) shouldBe true
    }

    "clear drops every element" {
        val subject = WeakSet<String>()

        subject.add("a")
        subject.add("b")

        subject.clear()

        subject.contains("a") shouldBe false
        subject.contains("b") shouldBe false
    }

    "a set can be used again after being cleared" {
        val subject = WeakSet<String>()

        subject.add("a")
        subject.clear()
        subject.add("b")

        subject.contains("a") shouldBe false
        subject.contains("b") shouldBe true
    }
})
