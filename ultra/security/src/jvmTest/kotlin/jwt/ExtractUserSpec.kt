package io.peekandpoke.ultra.security.jwt

import io.peekandpoke.ultra.common.model.Redacted
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.security.user.OrgId
import io.peekandpoke.ultra.security.user.UserId
import io.peekandpoke.ultra.security.user.UserPermissions
import io.peekandpoke.ultra.security.user.UserRecord
import kotlinx.serialization.json.JsonObject

/**
 * Pins the FAIL-CLOSED identity extraction.
 *
 * A JWT is attacker-supplied input. When its id claim (and `sub`) cannot produce a valid [UserId],
 * extraction must degrade to the anonymous subject — and drop the token's permissions with it —
 * rather than throw (which would turn a malformed token into a 500) or hand back a
 * nameless-yet-authenticated principal.
 *
 * Without these tests a refactor of `extract.kt`, or a change to [UserId]'s invariant, could
 * silently restore an authenticated principal with no identity. `AuthRule`'s `isAuthenticated`
 * derives from [UserRecord.isAnonymous], which is why that is asserted directly.
 */
class ExtractUserSpec : StringSpec({

    val ns = "user"
    val config = JwtConfig(
        issuer = "testIssuer",
        audience = "testAudience",
        signingKey = Redacted("test-signing-key-rfc7518-requires-sixty-four-bytes-minimum!!!!!!!"),
        permissionsNs = "permissions",
        userNs = ns,
    )
    val generator = JwtGenerator(config = config)

    /**
     * A decoded payload — extraction runs AFTER verification, so no signing is involved here.
     *
     * Built straight from the builder's claim map rather than through the verifier, because several
     * cases below deliberately carry claims a verifier would reject.
     */
    fun payloadOf(builder: JwtBuilder.() -> Unit): JwtPayload {
        return JwtPayload(claims = JsonObject(JwtBuilder().apply(builder).claims))
    }

    "a valid id claim yields that UserId" {
        val payload = payloadOf { withClaim("$ns/id", "b2b_users/u1") }

        payload.extractUser(ns).id shouldBe UserId("b2b_users/u1")
        payload.extractUser(ns).toUserRecord(null).isAnonymous() shouldBe false
    }

    "falls back to the subject when the id claim is absent" {
        val payload = payloadOf { withSubject("b2b_users/u1") }

        payload.extractUser(ns).id shouldBe UserId("b2b_users/u1")
    }

    "falls back to the subject when the id claim is PRESENT but invalid" {
        val payload = payloadOf {
            withClaim("$ns/id", "")
            withSubject("b2b_users/u1")
        }

        payload.extractUser(ns).id shouldBe UserId("b2b_users/u1")
    }

    "no id claim and no subject degrades to the anonymous subject" {
        val payload = payloadOf { withClaim("unrelated", "x") }

        payload.extractUser(ns).id shouldBe UserRecord.ANONYMOUS_ID
        // THE load-bearing assertion: AuthRule.isAuthenticated is !isAnonymous(), so this is what
        // makes an id-less token fail an `authenticated()` floor.
        payload.extractUser(ns).toUserRecord(null).isAnonymous() shouldBe true
    }

    "a blank, over-length, or control-character id degrades to the anonymous subject" {
        listOf(
            "",
            "   ",
            "x".repeat(UserId.MAX_LENGTH + 1),
            "b2b_users/u${0.toChar()}1",
        ).forEach { raw ->
            val payload = payloadOf { withClaim("$ns/id", raw) }

            payload.extractUser(ns).id shouldBe UserRecord.ANONYMOUS_ID
            payload.extractUser(ns).toUserRecord(null).isAnonymous() shouldBe true
        }
    }

    "a well-formed token carries its permissions through" {
        val token = generator.createJwt(
            user = JwtUserData(id = UserId("b2b_users/u1"), desc = "d", type = "t"),
            permissions = UserPermissions(isSuperUser = true, roles = setOf("admin")),
        )

        val subject = generator.extractUser(clientIp = "1.2.3.4", jwt = generator.verify(token))

        subject.record.isAnonymous() shouldBe false
        subject.permissions.isSuperUser shouldBe true
    }

    "the degraded principal drops the token's permissions along with its identity" {
        // Identity and permissions come from INDEPENDENT claim sets. A token carrying permission
        // claims but no usable id must not satisfy permission-only auth rules (isSuperUser(),
        // forRole(), ...), so the degradation has to be total.
        val idLess = generator.sign(
            JwtBuilder()
                .withIssuer(config.issuer)
                .withAudience(config.audience)
                .encodePermissions(config.permissionsNs, UserPermissions(isSuperUser = true, roles = setOf("admin")))
                .claims
        )

        val degraded = generator.extractUser(clientIp = "1.2.3.4", jwt = generator.verify(idLess))

        degraded.record.isAnonymous() shouldBe true
        degraded.permissions.isSuperUser shouldBe false
        degraded.permissions shouldBe UserPermissions.anonymous
    }

    "the builder CANNOT smuggle permission claims past the vetted UserPermissions" {
        // `createJwt` applies the caller's builder block BEFORE encodePermissions, and every write in
        // encodePermissions is conditional — so before `clearNamespace`, a builder-set claim survived
        // whenever the corresponding permission was absent. That is a privilege-escalation shape:
        // `createJwt(user, permissions = vetted) { withClaim("permissions/superuser", true) }` with an
        // unprivileged `vetted` produced a token that satisfied every permission-only auth rule.
        val token = generator.createJwt(
            user = JwtUserData(id = UserId("b2b_users/u1"), desc = "d", type = "t"),
            permissions = UserPermissions(),
            builder = {
                withClaim("permissions/superuser", true)
                withClaim("permissions/org", "organisation/attacker")
                withArrayClaim("permissions/roles", arrayOf("admin"))
                withClaim("user/id", "b2b_users/somebody-else")
            },
        )

        val permissions = generator.extractPermissions(generator.verify(token))

        permissions.isSuperUser shouldBe false
        permissions.org shouldBe null
        permissions.roles shouldBe emptySet()
        permissions shouldBe UserPermissions()

        // the user namespace is authoritative too
        generator.extractUserData(generator.verify(token)).id shouldBe UserId("b2b_users/u1")
    }

    "a LEGACY bare-_key org claim degrades to no selected org — fail-closed, not a 500" {
        // Every JWT minted before the org id became a collection-qualified `_id` carries `org: "acme"`.
        // Those tokens MUST NOT throw (a 500 on every request) and MUST NOT be honoured (that would be
        // the `_key`-vs-`_id` confusion this migration removed). They degrade to "no selected org",
        // which `hasOrganisation` then denies.
        val legacy = payloadOf {
            withClaim("permissions/org", "acme")
            withArrayClaim("permissions/accessibleOrgs", arrayOf("acme", "globex"))
        }

        val permissions = legacy.extractPermissions("permissions")

        permissions.org shouldBe null
        permissions.accessibleOrgs shouldBe emptySet()
        permissions.hasOrganisation(OrgId("organisation/acme")) shouldBe false
        permissions.canAccessOrg(OrgId("organisation/acme")) shouldBe false
    }

    "a well-formed org claim round-trips, and unparseable accessibleOrgs entries are dropped" {
        val mixed = payloadOf {
            withClaim("permissions/org", "organisation/acme")
            // one valid, one legacy bare key, one multi-segment
            withArrayClaim("permissions/accessibleOrgs", arrayOf("organisation/acme", "globex", "a/b/c"))
        }

        val permissions = mixed.extractPermissions("permissions")

        permissions.org shouldBe OrgId("organisation/acme")
        permissions.accessibleOrgs shouldBe setOf(OrgId("organisation/acme"))
    }
})
