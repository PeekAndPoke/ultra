package io.peekandpoke.ultra.common

import io.kotest.core.spec.style.StringSpec
import io.peekandpoke.ultra.common.recursion.recurse
import io.kotest.matchers.shouldBe

private enum class FixEnum { ONE, TWO }

/** Regressions for the 2026-07-29 backlog sweep of `ultra/common`. */
class CommonFixesSpec : StringSpec({

    // ellipsis ////////////////////////////////////////////////////////////////////////////////////

    "ellipsis never splits a surrogate pair" {
        // one emoji is two UTF-16 code units, so a cut at 1 used to leave a lone high surrogate
        val emoji = "😀😀"

        emoji.ellipsis(1) shouldBe "..."
        emoji.ellipsis(2) shouldBe "😀..."
        emoji.ellipsis(3) shouldBe "😀..."
    }

    "ellipsis clamps a negative maxLength instead of throwing" {
        "abc".ellipsis(-1) shouldBe "..."
        "abc".ellipsis(-100, "~") shouldBe "~"
    }

    "ellipsis keeps its long-standing contract" {
        // maxLength bounds the kept text, the suffix comes on top - unchanged on purpose
        "Some Text".ellipsis(4) shouldBe "Some..."
        "hello world".ellipsis(5) shouldBe "hello..."
        "exact".ellipsis(5) shouldBe "exact"
    }

    // safeEnumsOf / safeEnumOf ////////////////////////////////////////////////////////////////////

    "safeEnumsOf keeps repeats and input order" {
        safeEnumsOf<FixEnum>("ONE,ONE,TWO") shouldBe listOf(FixEnum.ONE, FixEnum.ONE, FixEnum.TWO)
        safeEnumsOf<FixEnum>("TWO,ONE") shouldBe listOf(FixEnum.TWO, FixEnum.ONE)
    }

    "safeEnumsOf still trims and drops blanks and unknowns" {
        safeEnumsOf<FixEnum>(" ONE , ,NOPE, TWO ") shouldBe listOf(FixEnum.ONE, FixEnum.TWO)
        safeEnumsOf<FixEnum>(null) shouldBe emptyList()
    }

    "safeEnumOrNull answers a miss without raising" {
        safeEnumOrNull<FixEnum>("NOPE") shouldBe null
        safeEnumOrNull<FixEnum>("one") shouldBe null   // case sensitive
        safeEnumOrNull<FixEnum>("ONE") shouldBe FixEnum.ONE
        safeEnumOf("NOPE", FixEnum.TWO) shouldBe FixEnum.TWO
    }

    // containsAny /////////////////////////////////////////////////////////////////////////////////

    "containsAny short-circuits but keeps its answers" {
        listOf(1, 2, 3).containsAny(listOf(3, 9)) shouldBe true
        listOf(1, 2, 3).containsAny(listOf(9)) shouldBe false
        listOf(1, 2, 3).containsAny(emptyList()) shouldBe false
        emptyList<Int>().containsAny(listOf(1)) shouldBe false
        listOf(1, 2, 3).containsNone(listOf(9)) shouldBe true
    }

    // recurse /////////////////////////////////////////////////////////////////////////////////////

    "recurse walks a chain and stops on a cycle" {
        val chain = mapOf("c" to "b", "b" to "a")

        "c".recurse { chain[this] } shouldBe listOf("c", "b", "a")

        val cyclic = mapOf("a" to "b", "b" to "a")
        "a".recurse { cyclic[this] } shouldBe listOf("a", "b")
    }

    // TypedAttributes /////////////////////////////////////////////////////////////////////////////

    "TypedAttributes is built through the builder" {
        val key = TypedKey<String>("k")

        val subject = TypedAttributes { add(key, "value") }

        subject[key] shouldBe "value"
        subject.size shouldBe 1
        // size and entries can no longer disagree - both come from the same built map
        subject.entries.size shouldBe subject.size
    }

    // GetAndSet equality //////////////////////////////////////////////////////////////////////////

    "GetAndSet equality is symmetric" {
        var a = 1
        var b = 1
        val one = GetAndSet.of(getter = { a }, setter = { a = it; it })
        val two = GetAndSet.of(getter = { b }, setter = { b = it; it })

        (one == two) shouldBe true
        (two == one) shouldBe true

        two.set(2)

        (one == two) shouldBe false
        (two == one) shouldBe false
    }
})
