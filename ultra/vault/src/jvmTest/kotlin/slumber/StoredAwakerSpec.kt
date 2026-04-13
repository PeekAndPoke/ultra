package io.peekandpoke.ultra.vault.slumber

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.peekandpoke.ultra.reflection.kType
import io.peekandpoke.ultra.slumber.Codec
import io.peekandpoke.ultra.slumber.SlumberConfig
import io.peekandpoke.ultra.slumber.awake
import io.peekandpoke.ultra.vault.Stored

class StoredAwakerSpec : StringSpec({

    val codec = Codec(
        config = SlumberConfig.default.prependModules(VaultSlumberModule)
    )

    // awake Stored from a Map /////////////////////////////////////////////////////////////////////

    "awake Stored<AwakerUser> from a full map populates value and metadata" {
        val data = mapOf(
            "_id" to "users/ada",
            "_key" to "ada",
            "_rev" to "rev1",
            "name" to "Ada",
            "age" to 36,
        )

        val result = codec.awake<Stored<AwakerUser>>(data)!!

        result._id shouldBe "users/ada"
        result._key shouldBe "ada"
        result._rev shouldBe "rev1"
        result.resolve() shouldBe AwakerUser(name = "Ada", age = 36)
    }

    "awake Stored derives _key from _id when _key is missing" {
        val data = mapOf(
            "_id" to "users/grace",
            "_rev" to "rev1",
            "name" to "Grace",
            "age" to 42,
        )

        val result = codec.awake<Stored<AwakerUser>>(data)!!

        result._id shouldBe "users/grace"
        result._key shouldBe "grace"
    }

    "awake Stored defaults _rev to empty string when missing" {
        val data = mapOf(
            "_id" to "users/linus",
            "_key" to "linus",
            "name" to "Linus",
            "age" to 55,
        )

        val result = codec.awake<Stored<AwakerUser>>(data)!!

        result._rev shouldBe ""
    }

    // Invalid input ///////////////////////////////////////////////////////////////////////////////

    "awake returns null when _id is missing" {
        val data = mapOf(
            "_key" to "ada",
            "name" to "Ada",
            "age" to 36,
        )

        val result = codec.awake<Stored<AwakerUser>>(data)

        result.shouldBeNull()
    }

    "awake returns null when _id is present but not a String" {
        val data = mapOf(
            "_id" to 123,
            "_key" to "ada",
            "name" to "Ada",
            "age" to 36,
        )

        val result = codec.awake<Stored<AwakerUser>>(data)

        result.shouldBeNull()
    }

    "awake returns null for non-Map input (direct call)" {
        val awaker = StoredAwaker(innerType = kType<AwakerUser>().type)

        val result = awaker.awake(
            data = "not a map",
            context = codec.firstPassAwakerContext,
        )

        result.shouldBeNull()
    }

    "awake returns null for null input (direct call)" {
        val awaker = StoredAwaker(innerType = kType<AwakerUser>().type)

        val result = awaker.awake(
            data = null,
            context = codec.firstPassAwakerContext,
        )

        result.shouldBeNull()
    }

    // Returned type shape /////////////////////////////////////////////////////////////////////////

    "awake returns a Stored instance (not Ref or New)" {
        val data = mapOf(
            "_id" to "users/ada",
            "_key" to "ada",
            "_rev" to "rev1",
            "name" to "Ada",
            "age" to 36,
        )

        val result = codec.awake<Stored<AwakerUser>>(data)

        result.shouldBeInstanceOf<Stored<*>>()
    }
})

// Test fixtures ///////////////////////////////////////////////////////////////////////////////////

internal data class AwakerUser(val name: String, val age: Int)
