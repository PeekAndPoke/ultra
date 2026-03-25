package io.peekandpoke.ultra.vault

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class StorableSpec : StringSpec({

    val stored = Stored(value = "hello", _id = "col/123", _key = "123", _rev = "rev1")

    // Stored /////////////////////////////////////////////////////////////////////////////////

    "Stored.collection extracts collection name from _id" {
        stored.collection shouldBe "col"
    }

    "Stored.modify maps the value" {
        val modified = stored.modify { it.uppercase() }

        modified.value shouldBe "HELLO"
        modified._id shouldBe "col/123"
        modified._key shouldBe "123"
        modified._rev shouldBe "rev1"
    }

    "Stored.withValue replaces the value, keeps metadata" {
        val replaced = stored.withValue("world")

        replaced.value shouldBe "world"
        replaced._id shouldBe stored._id
    }

    "Stored.transform maps to a different type" {
        val transformed = stored.transform { it.length }

        transformed.value shouldBe 5
        transformed._id shouldBe stored._id
    }

    "Stored.asRef converts to Ref" {
        val ref = stored.asRef

        ref.shouldBeInstanceOf<Ref<String>>()
        ref.value shouldBe "hello"
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

    "Ref.modify maps the value" {
        val ref = Ref(value = 10, _id = "c/1", _key = "1", _rev = "r")
        val modified = ref.modify { it * 2 }

        modified.value shouldBe 20
        modified._id shouldBe "c/1"
    }

    // New ////////////////////////////////////////////////////////////////////////////////////

    "New has empty defaults" {
        val new = New(value = "data")

        new._id shouldBe ""
        new._key shouldBe ""
        new._rev shouldBe ""
    }

    "New.modify maps the value" {
        val new = New(value = "hello")
        val modified = new.modify { it.uppercase() }

        modified.value shouldBe "HELLO"
    }
})
