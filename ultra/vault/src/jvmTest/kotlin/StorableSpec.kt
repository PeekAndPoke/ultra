package io.peekandpoke.ultra.vault

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class StorableSpec : StringSpec({

    val stored = Stored(value = "hello", _id = "col/123", _key = "123", _rev = "rev1")

    // Stored /////////////////////////////////////////////////////////////////////////////////

    "Stored.resolve returns value instantly" {
        stored.resolve() shouldBe "hello"
    }

    "Stored.invoke returns value instantly" {
        stored() shouldBe "hello"
    }

    "Stored.collection extracts collection name from _id" {
        stored.collection shouldBe "col"
    }

    "Stored.modify maps the value" {
        val modified = stored.modify { it.uppercase() }

        modified.resolve() shouldBe "HELLO"
        modified._id shouldBe "col/123"
        modified._key shouldBe "123"
        modified._rev shouldBe "rev1"
    }

    "Stored.withValue replaces the value, keeps metadata" {
        val replaced = stored.withValue("world")

        replaced.resolve() shouldBe "world"
        replaced._id shouldBe stored._id
    }

    "Stored.transform maps to a different type" {
        val transformed = stored.transform { it.length }

        transformed.resolve() shouldBe 5
        transformed._id shouldBe stored._id
    }

    "Stored.asRef converts to Ref" {
        val ref = stored.asRef

        ref.shouldBeInstanceOf<Ref<String>>()
        ref.resolve() shouldBe "hello"
        ref._id shouldBe stored._id
    }

    "Stored.asStored returns equivalent Stored" {
        stored.asStored shouldBe stored
    }

    "Stored.castTyped returns typed when match" {
        val any: Stored<Any> = Stored(value = "text", _id = "c/1", _key = "1", _rev = "")
        val cast = any.castTyped<String>()

        cast shouldBe any
    }

    "Stored.castTyped returns null when no match" {
        val any: Stored<Any> = Stored(value = 42, _id = "c/1", _key = "1", _rev = "")
        val cast = any.castTyped<String>()

        cast shouldBe null
    }

    // hasSameIdAs / hasOtherIdThan / hasIdIn ///////////////////////////////////////////////////

    "hasSameIdAs returns true for same id" {
        val other = Stored(value = "other", _id = "col/123", _key = "123", _rev = "rev2")

        (stored hasSameIdAs other) shouldBe true
    }

    "hasSameIdAs returns false for different id" {
        val other = Stored(value = "other", _id = "col/999", _key = "999", _rev = "")

        (stored hasSameIdAs other) shouldBe false
    }

    "hasSameIdAs returns false for null" {
        (stored hasSameIdAs null) shouldBe false
    }

    "hasOtherIdThan is inverse of hasSameIdAs" {
        val same = Stored(value = "x", _id = "col/123", _key = "123", _rev = "")
        val different = Stored(value = "x", _id = "col/999", _key = "999", _rev = "")

        (stored hasOtherIdThan same) shouldBe false
        (stored hasOtherIdThan different) shouldBe true
    }

    "hasIdIn checks against list of storables" {
        val list = listOf(
            Stored(value = "a", _id = "col/100", _key = "100", _rev = ""),
            Stored(value = "b", _id = "col/123", _key = "123", _rev = ""),
        )

        (stored hasIdIn list) shouldBe true
        (Stored(value = "x", _id = "col/999", _key = "999", _rev = "") hasIdIn list) shouldBe false
    }

    // Ref ////////////////////////////////////////////////////////////////////////////////////

    "Ref.eager creates an immediately resolvable ref" {
        val ref = Ref.eager(value = "hello", _id = "c/1", _key = "1", _rev = "r")

        ref.resolve() shouldBe "hello"
        ref._id shouldBe "c/1"
    }

    "Ref.lazy creates a ref resolved on first access" {
        var callCount = 0
        val ref = Ref.lazy<String>("c/1") {
            callCount++
            Stored("resolved", "c/1", "1", "r")
        }

        callCount shouldBe 0
        ref.resolve() shouldBe "resolved"
        callCount shouldBe 1
    }

    "Ref.resolve caches — resolver called only once" {
        var callCount = 0
        val ref = Ref.lazy<String>("c/1") {
            callCount++
            Stored("resolved", "c/1", "1", "r")
        }

        ref.resolve() shouldBe "resolved"
        ref.resolve() shouldBe "resolved"
        ref.resolve() shouldBe "resolved"
        callCount shouldBe 1
    }

    "Ref.invoke is shorthand for resolve" {
        val ref = Ref.eager(value = "hello", _id = "c/1", _key = "1", _rev = "r")

        ref() shouldBe "hello"
    }

    "Ref equality is based on _id only" {
        val ref1 = Ref.eager(value = "a", _id = "c/1", _key = "1", _rev = "r1")
        val ref2 = Ref.eager(value = "b", _id = "c/1", _key = "1", _rev = "r2")
        val ref3 = Ref.eager(value = "a", _id = "c/2", _key = "2", _rev = "r1")

        (ref1 == ref2) shouldBe true
        (ref1 == ref3) shouldBe false
        ref1.hashCode() shouldBe ref2.hashCode()
    }

    "Ref.modify maps the value" {
        val ref = Ref.eager(value = 10, _id = "c/1", _key = "1", _rev = "r")
        val modified = ref.modify { it * 2 }

        modified.resolve() shouldBe 20
        modified._id shouldBe "c/1"
    }

    "Ref.withValue replaces the value eagerly" {
        val ref = Ref.eager(value = "old", _id = "c/1", _key = "1", _rev = "r")
        val replaced = ref.withValue("new")

        replaced.resolve() shouldBe "new"
        replaced._id shouldBe "c/1"
    }

    "Ref.transform creates new lazy ref" {
        val ref = Ref.eager(value = "hello", _id = "c/1", _key = "1", _rev = "r")
        val transformed = ref.transform { it.length }

        transformed.resolve() shouldBe 5
        transformed._id shouldBe "c/1"
    }

    "Ref.resolve is safe under concurrent access — resolver called exactly once" {
        var callCount = 0
        val ref = Ref.lazy<String>("c/1") {
            callCount++
            Stored("resolved", "c/1", "1", "r")
        }

        coroutineScope {
            val results = (1..100).map {
                async { ref.resolve() }
            }.awaitAll()

            results.forEach { it shouldBe "resolved" }
        }

        callCount shouldBe 1
    }

    "Ref.valueInternal throws when not yet resolved" {
        val ref = Ref.lazy<String>("c/1") { Stored("x", "c/1", "1", "r") }

        shouldThrow<IllegalStateException> {
            @Suppress("UNCHECKED_CAST")
            (ref as Storable<String>).valueInternal
        }
    }

    // New ////////////////////////////////////////////////////////////////////////////////////

    "New.resolve returns value instantly" {
        val new = New(value = "data")
        new.resolve() shouldBe "data"
    }

    "New.invoke returns value instantly" {
        val new = New(value = "data")
        new() shouldBe "data"
    }

    "New has empty defaults" {
        val new = New(value = "data")

        new._id shouldBe ""
        new._key shouldBe ""
        new._rev shouldBe ""
    }

    "New.modify maps the value" {
        val new = New(value = "hello")
        val modified = new.modify { it.uppercase() }

        modified.resolve() shouldBe "HELLO"
    }
})
