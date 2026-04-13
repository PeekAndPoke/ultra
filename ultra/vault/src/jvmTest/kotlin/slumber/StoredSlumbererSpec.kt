package io.peekandpoke.ultra.vault.slumber

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.slumber.Codec
import io.peekandpoke.ultra.slumber.SlumberConfig
import io.peekandpoke.ultra.slumber.slumber
import io.peekandpoke.ultra.vault.New
import io.peekandpoke.ultra.vault.Stored
import io.peekandpoke.ultra.vault.VaultException

class StoredSlumbererSpec : StringSpec({

    val codec = Codec(
        config = SlumberConfig.default.prependModules(VaultSlumberModule)
    )

    // slumber Stored //////////////////////////////////////////////////////////////////////////////

    "slumber Stored<User> returns a flat map with user fields plus _id and _key" {
        val stored = Stored(
            value = SlumberUser(name = "Ada", age = 36),
            _id = "users/ada",
            _key = "ada",
            _rev = "rev1",
        )

        val result = codec.slumber(stored) as Map<*, *>

        @Suppress("UNCHECKED_CAST")
        (result as Map<String, Any?>) shouldContainExactly mapOf(
            "name" to "Ada",
            "age" to 36,
            "_id" to "users/ada",
            "_key" to "ada",
        )
    }

    "slumber Stored omits _id and _key when empty" {
        val stored = Stored(
            value = SlumberUser(name = "Grace", age = 42),
            _id = "",
            _key = "",
            _rev = "",
        )

        val result = codec.slumber(stored) as Map<*, *>

        result.containsKey("_id") shouldBe false
        result.containsKey("_key") shouldBe false
        result["name"] shouldBe "Grace"
        result["age"] shouldBe 42
    }

    // slumber New /////////////////////////////////////////////////////////////////////////////////

    "slumber New<User> with empty metadata produces plain user map" {
        val new = New(value = SlumberUser(name = "Linus", age = 55))

        val result = codec.slumber(new) as Map<*, *>

        @Suppress("UNCHECKED_CAST")
        (result as Map<String, Any?>) shouldContainExactly mapOf(
            "name" to "Linus",
            "age" to 55,
        )
    }

    "slumber New<User> with pre-assigned _id and _key includes them" {
        val new = New(
            value = SlumberUser(name = "Dennis", age = 70),
            _id = "users/dennis",
            _key = "dennis",
        )

        val result = codec.slumber(new) as Map<*, *>

        result["_id"] shouldBe "users/dennis"
        result["_key"] shouldBe "dennis"
    }

    // Non-Storable input //////////////////////////////////////////////////////////////////////////

    "slumber returns null when called with a non-Storable value directly" {
        val result = StoredSlumberer.slumber(
            data = "not a storable",
            context = codec.firstPassSlumbererContext,
        )

        result.shouldBeNull()
    }

    "slumber returns null when called with null data directly" {
        val result = StoredSlumberer.slumber(
            data = null,
            context = codec.firstPassSlumbererContext,
        )

        result.shouldBeNull()
    }

    // Inner must be a Map /////////////////////////////////////////////////////////////////////////

    "slumber throws VaultException when inner value does not serialize to a Map" {
        // Inner type is String, which slumbers to a String (not a Map)
        val stored = Stored(
            value = "raw string",
            _id = "things/x",
            _key = "x",
            _rev = "",
        )

        val ex = shouldThrow<VaultException> {
            codec.slumber(stored)
        }

        ex.message!!.contains("Expected slumbered value to be a Map") shouldBe true
    }
})

// Test fixtures ///////////////////////////////////////////////////////////////////////////////////

internal data class SlumberUser(val name: String, val age: Int)
