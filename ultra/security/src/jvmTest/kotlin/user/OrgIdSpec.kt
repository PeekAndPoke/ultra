package io.peekandpoke.ultra.security.user

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.peekandpoke.ultra.slumber.Codec
import io.peekandpoke.ultra.slumber.awake
import io.peekandpoke.ultra.slumber.slumber
import kotlinx.serialization.descriptors.PrimitiveKind

class OrgIdSpec : StringSpec({

    "init REJECTS a bare _key — this is what makes the migration self-checking" {
        // The whole point: a site that still hands over a bare `_key` blows up at construction
        // instead of silently matching nothing and 404ing every org-scoped request.
        shouldThrow<IllegalArgumentException> { OrgId("acme") }
    }

    "init rejects a multi-segment id, so `key` is never ambiguous" {
        shouldThrow<IllegalArgumentException> { OrgId("organisation/acme/extra") }
    }

    "init rejects an empty collection or key" {
        shouldThrow<IllegalArgumentException> { OrgId("/acme") }
        shouldThrow<IllegalArgumentException> { OrgId("organisation/") }
        shouldThrow<IllegalArgumentException> { OrgId("/") }
    }

    "init rejects blank, over-length, and control-character values" {
        shouldThrow<IllegalArgumentException> { OrgId("") }
        shouldThrow<IllegalArgumentException> { OrgId("   ") }
        shouldThrow<IllegalArgumentException> { OrgId("organisation/" + "x".repeat(OrgId.MAX_LENGTH)) }
        shouldThrow<IllegalArgumentException> { OrgId("organisation/a${0.toChar()}b") }
    }

    "init accepts a well-formed collection/key id" {
        OrgId("organisation/acme").value shouldBe "organisation/acme"
        OrgId.of("organisation", "acme") shouldBe OrgId("organisation/acme")
    }

    "key and collection project the two halves" {
        val subject = OrgId("organisation/acme")

        subject.key shouldBe "acme"
        subject.collection shouldBe "organisation"
    }

    "key is what a URL segment takes — the ONE place a bare key still belongs" {
        // /api/b2b/orgs/{org}/members  ->  the route param's TYPE supplies the collection.
        "/api/b2b/orgs/${OrgId("organisation/acme").key}/members" shouldBe "/api/b2b/orgs/acme/members"
    }

    "equality and hashCode are by value" {
        OrgId("organisation/acme") shouldBe OrgId("organisation/acme")
        OrgId("organisation/acme") shouldNotBe OrgId("organisation/globex")
        OrgId("organisation/acme").hashCode() shouldBe OrgId("organisation/acme").hashCode()
    }

    "two orgs with the same key in DIFFERENT collections are not equal" {
        OrgId("organisation/acme") shouldNotBe OrgId("other_orgs/acme")
    }

    "toString returns the bare value" {
        OrgId("organisation/acme").toString() shouldBe "organisation/acme"
    }

    "parseOrNull degrades instead of throwing, for attacker-supplied claims" {
        OrgId.parseOrNull("organisation/acme") shouldBe OrgId("organisation/acme")
        OrgId.parseOrNull(null) shouldBe null
        OrgId.parseOrNull("") shouldBe null
        OrgId.parseOrNull("acme") shouldBe null                     // a bare key in a JWT claim
        OrgId.parseOrNull("organisation/a/b") shouldBe null
    }

    "serializes as a plain string in kotlinx" {
        OrgId.serializer().descriptor.isInline shouldBe true
        OrgId.serializer().descriptor.getElementDescriptor(0).kind shouldBe PrimitiveKind.STRING
    }

    "slumber round-trips it as a bare scalar, and init runs on decode" {
        val codec = Codec.default

        codec.slumber(OrgId("organisation/acme")) shouldBe "organisation/acme"
        codec.awake<OrgId>("organisation/acme") shouldBe OrgId("organisation/acme")

        // A bare key that somehow reached storage or the wire is REJECTED on the way in.
        shouldThrow<Throwable> { codec.awake<OrgId>("acme") }
    }
})
