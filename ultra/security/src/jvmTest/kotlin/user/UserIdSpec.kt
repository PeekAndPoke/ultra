package io.peekandpoke.ultra.security.user

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.peekandpoke.ultra.slumber.Codec
import io.peekandpoke.ultra.slumber.awake
import io.peekandpoke.ultra.slumber.slumber
import kotlinx.serialization.descriptors.PrimitiveKind

class UserIdSpec : StringSpec({

    "init rejects blank, over-length, and control-character values" {
        shouldThrow<IllegalArgumentException> { UserId("") }
        shouldThrow<IllegalArgumentException> { UserId("   ") }
        shouldThrow<IllegalArgumentException> { UserId("x".repeat(UserId.MAX_LENGTH + 1)) }
        // Control characters must not be smuggled in — composite cache keys and log lines use
        // delimiters like NUL, so an id carrying one could forge a key boundary.
        shouldThrow<IllegalArgumentException> { UserId("a${0.toChar()}b") }
        shouldThrow<IllegalArgumentException> { UserId("a\nb") }
        shouldThrow<IllegalArgumentException> { UserId("ab") }
    }

    "init accepts the realm-qualified collection/key form — the canonical shape" {
        // The slash MUST be allowed: every persisted user reference is a Vault `_id`.
        UserId("b2b_users/abc123").value shouldBe "b2b_users/abc123"
        UserId("b2b2c_users/xyz").value shouldBe "b2b2c_users/xyz"
    }

    "init accepts the synthetic, non-document subjects that share this type" {
        // These are not documents at all, which is why the invariant cannot require a slash.
        UserId("anonymous") shouldBe UserRecord.ANONYMOUS_ID
        UserId("system") shouldBe UserRecord.SYSTEM_ID
        // The access-estimation probe (ApiRoute.estimateAccess) and opaque API-key subjects.
        UserId("role-eval").value shouldBe "role-eval"
    }

    "equality and hashCode are by value" {
        UserId("b2b_users/u1") shouldBe UserId("b2b_users/u1")
        UserId("b2b_users/u1") shouldNotBe UserId("b2b_users/u2")
        UserId("b2b_users/u1").hashCode() shouldBe UserId("b2b_users/u1").hashCode()
    }

    "a bare key and a collection-qualified id are NOT equal" {
        // Guards the coll/key convention: the two id spaces must never compare equal by accident.
        UserId("b2b_users/u1") shouldNotBe UserId("u1")
    }

    "toString returns the bare value (clean logs / interpolation)" {
        UserId("b2b_users/u1").toString() shouldBe "b2b_users/u1"
    }

    "serializes as a plain string, so wire and storage are unchanged from userId: String" {
        // An inline value class over a String encodes as that bare string — no wrapper object — so
        // this migration is NOT a data migration. (Only serialization-core is on this module's test
        // classpath, so assert the descriptor rather than round-tripping through Json.)
        UserId.serializer().descriptor.isInline shouldBe true
        UserId.serializer().descriptor.getElementDescriptor(0).kind shouldBe PrimitiveKind.STRING
    }

    "slumber round-trips it as a bare scalar — the codec that carries it to the wire and both DBs" {
        val codec = Codec.default

        codec.slumber(UserId("b2b_users/u1")) shouldBe "b2b_users/u1"
        codec.awake<UserId>("b2b_users/u1") shouldBe UserId("b2b_users/u1")
    }

    "the init invariant RUNS ON DECODE, so a bad stored/wire value is rejected not resurrected" {
        // This is the whole security argument for putting invariants in `init {}` rather than a
        // factory: slumber constructs via the ctor, so `require` fires on the way IN as well.
        val codec = Codec.default

        shouldThrow<Throwable> { codec.awake<UserId>("") }
        shouldThrow<Throwable> { codec.awake<UserId>("b2b_users/u${0.toChar()}1") }
    }

    "the sentinel predicates still identify the synthetic actors" {
        UserRecord.Anonymous().isAnonymous() shouldBe true
        UserRecord.Anonymous().isSystem() shouldBe false
        UserRecord.System().isSystem() shouldBe true
        UserRecord.LoggedIn(userId = UserId("b2b_users/u1")).isAnonymous() shouldBe false
        UserRecord.LoggedIn(userId = UserId("b2b_users/u1")).isSystem() shouldBe false
    }
})
