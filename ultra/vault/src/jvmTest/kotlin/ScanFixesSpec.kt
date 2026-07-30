package io.peekandpoke.ultra.vault

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.vault.profiling.QueryProfiler

/**
 * Regressions for the defects found by the 2026-07-28 vault scan.
 * See `.claude/tasks-archive/2026-07/20260728-vault-scan-findings.md`.
 */
class ScanFixesSpec : StringSpec({

    // Ref.eager pre-seeds the cache ///////////////////////////////////////////////////////////////

    "asRef exposes the value without a suspend resolve" {
        val stored = Stored(value = "hello", _id = "col/1", _key = "1", _rev = "rev1")

        // used to throw IllegalStateException("Ref(col/1) not yet resolved")
        stored.asRef.asStored._rev shouldBe "rev1"
    }

    "asRef preserves the revision" {
        val stored = Stored(value = "hello", _id = "col/1", _key = "1", _rev = "rev1")

        stored.asRef._rev shouldBe "rev1"
    }

    "asRef preserves a key that differs from the one embedded in the id" {
        val stored = Stored(value = "hello", _id = "col/1", _key = "weird", _rev = "rev1")

        stored.asRef._key shouldBe "weird"
    }

    "an unresolved lazy Ref still derives its key from the id" {
        val ref = Ref.lazy<String>("col/abc") { Stored("v", "col/abc", "abc", "") }

        ref._key shouldBe "abc"
    }

    "an unresolved lazy Ref still refuses non-suspend value access" {
        val ref = Ref.lazy<String>("col/1") { Stored("v", "col/1", "1", "") }

        shouldThrow<IllegalStateException> { ref.asStored }
    }

    // Identity of unsaved entities ////////////////////////////////////////////////////////////////

    "two unsaved entities are not the same entity" {
        val a = New("userA")
        val b = New("userB")

        (a hasSameIdAs b) shouldBe false
        (a hasOtherIdThan b) shouldBe true
    }

    "an unsaved entity matches nothing in a list" {
        New("userA") hasIdIn listOf(New("userB"), New("userC")) shouldBe false
    }

    "two unsaved Refs are not equal and do not collapse in a Set" {
        val a = New("userA").asRef
        val b = New("userB").asRef

        (a == b) shouldBe false
        setOf(a, b).size shouldBe 2
    }

    "Refs to the same row are still equal" {
        val a = Ref.eager("valueA", "col/1", "1", "r1")
        val b = Ref.eager("valueB", "col/1", "1", "r2")

        // id-only equality is deliberate for a lazy reference — see StorableSpec
        (a == b) shouldBe true
        setOf(a, b).size shouldBe 1
    }

    "an unsaved Ref is equal to itself" {
        val a = New("userA").asRef

        // reflexivity: the empty-id guard must not break x == x
        (a == a) shouldBe true
        listOf(a).contains(a) shouldBe true
    }

    "saved entities still compare by id" {
        val a = Stored("v", "col/1")
        val b = Stored("other", "col/1")
        val c = Stored("v", "col/2")

        (a hasSameIdAs b) shouldBe true
        (a hasSameIdAs c) shouldBe false
        a hasIdIn listOf(c, b) shouldBe true
    }

    // Profiler records timings even when the block throws /////////////////////////////////////////

    "a stop watch records the elapsed time when the block throws" {
        val sw = QueryProfiler.StopWatch.Impl()

        shouldThrow<IllegalStateException> {
            sw { error("query failed") }
        }

        sw.count shouldBe 1
    }

    "a suspending stop watch records the elapsed time when the block throws" {
        val sw = QueryProfiler.StopWatch.Impl()

        shouldThrow<IllegalStateException> {
            sw.async { error("query failed") }
        }

        sw.count shouldBe 1
    }
})
