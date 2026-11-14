package io.peekandpoke.ultra.common

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

/** ULTRA-00341: [ComputedList], a read-only list view that computes each element on demand. */
@Suppress("unused")
class ComputedListSpec : StringSpec({

    "an empty view" {
        val subject = ComputedList(0) { it }

        subject.size shouldBe 0
        subject.isEmpty() shouldBe true
        subject.iterator().hasNext() shouldBe false
        subject shouldBe emptyList()
    }

    "a negative size is an empty view, no exception" {
        ComputedList(-5) { it }.size shouldBe 0
    }

    "get computes the element for an index; out of range is the List contract's IndexOutOfBoundsException" {
        val subject = ComputedList(3) { it * 10 }

        listOf(subject[0], subject[1], subject[2]) shouldBe listOf(0, 10, 20)
        shouldThrow<IndexOutOfBoundsException> { subject[-1] }
        shouldThrow<IndexOutOfBoundsException> { subject[3] }
    }

    "iteration, first and last follow the index order" {
        val subject = ComputedList(4) { "e$it" }

        subject.toList() shouldBe listOf("e0", "e1", "e2", "e3")
        subject.first() shouldBe "e0"
        subject.last() shouldBe "e3"
    }

    "nothing is computed until read; each read computes again" {
        var computed = 0
        val subject = ComputedList(1_000) { computed++; it }

        computed shouldBe 0
        subject[500]
        computed shouldBe 1
        subject[500]
        computed shouldBe 2
        subject.size
        computed shouldBe 2
    }

    "subList is a view: building it computes nothing" {
        var computed = 0
        val subject = ComputedList(1_000) { computed++; it }

        val view = subject.subList(100, 200)

        computed shouldBe 0
        view.size shouldBe 100
        view[0] shouldBe 100
        computed shouldBe 1
    }

    "equals, hashCode, contains and indexOf behave like any list" {
        val subject = ComputedList(3) { it + 1 }

        subject shouldBe listOf(1, 2, 3)
        subject.hashCode() shouldBe listOf(1, 2, 3).hashCode()
        (2 in subject) shouldBe true
        (4 in subject) shouldBe false
        subject.indexOf(3) shouldBe 2
    }

    "it is RandomAccess, so the cache's size estimator samples it by index" {
        (ComputedList(10) { it } is RandomAccess) shouldBe true
    }

    "a view of Int.MAX_VALUE elements is free to create and cheap to read at its ends" {
        val subject = ComputedList(Int.MAX_VALUE) { it.toLong() * 2 }

        subject.size shouldBe Int.MAX_VALUE
        subject.first() shouldBe 0L
        subject.last() shouldBe (Int.MAX_VALUE - 1).toLong() * 2
    }

    "toList builds a real list" {
        val copy = ComputedList(3) { it }.toList()

        copy shouldBe listOf(0, 1, 2)
        (copy is ComputedList<*>) shouldBe false
        copy.shouldBeInstanceOf<List<Int>>()
    }
})
