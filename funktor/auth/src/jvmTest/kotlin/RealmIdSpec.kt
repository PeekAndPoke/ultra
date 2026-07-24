package io.peekandpoke.funktor.auth

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.peekandpoke.funktor.auth.model.RealmId
import kotlinx.serialization.json.Json

class RealmIdSpec : StringSpec({

    "init rejects empty, blank, out-of-charset, and over-length values" {
        shouldThrow<IllegalArgumentException> { RealmId("") }
        shouldThrow<IllegalArgumentException> { RealmId("   ") }
        shouldThrow<IllegalArgumentException> { RealmId("a b") }        // space
        shouldThrow<IllegalArgumentException> { RealmId("a/b") }        // slash
        shouldThrow<IllegalArgumentException> { RealmId("a:b") }        // colon
        // the NUL char is the session-cache-key delimiter — must be rejected
        shouldThrow<IllegalArgumentException> { RealmId("a${0.toChar()}b") }
        shouldThrow<IllegalArgumentException> { RealmId("x".repeat(RealmId.MAX_LENGTH + 1)) }
    }

    "init accepts the real realm identifiers" {
        // must not throw
        listOf("b2b", "b2b2c", "operators", "admin-user", "test-realm").forEach { RealmId(it) }
    }

    "equality and hashCode are by value" {
        RealmId("b2b") shouldBe RealmId("b2b")
        RealmId("b2b") shouldNotBe RealmId("ops")
        RealmId("b2b").hashCode() shouldBe RealmId("b2b").hashCode()
    }

    "toString returns the bare value (clean logs / interpolation)" {
        RealmId("b2b").toString() shouldBe "b2b"
    }

    "serializes as a plain string, so the wire format is unchanged from realm: String" {
        Json.encodeToString(RealmId.serializer(), RealmId("b2b")) shouldBe "\"b2b\""
        Json.decodeFromString(RealmId.serializer(), "\"b2b\"") shouldBe RealmId("b2b")
    }
})
