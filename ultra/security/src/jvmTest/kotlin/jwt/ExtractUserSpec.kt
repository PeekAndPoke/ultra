package io.peekandpoke.ultra.security.jwt

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTCreator
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.Payload
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.security.user.UserId
import io.peekandpoke.ultra.security.user.UserPermissions
import io.peekandpoke.ultra.security.user.UserRecord

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
        signingKey = "testSigningKey",
        permissionsNs = "permissions",
        userNs = ns,
    )
    val generator = JwtGenerator(config = config)

    /** A decoded (signature-irrelevant) payload — extraction runs after verification. */
    fun payloadOf(builder: JWTCreator.Builder.() -> Unit): Payload =
        JWT.decode(JWT.create().apply(builder).sign(Algorithm.HMAC512(config.signingKey)))

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
        val idLess = JWT.create()
            .withIssuer(config.issuer)
            .withAudience(config.audience)
            .encodePermissions(config.permissionsNs, UserPermissions(isSuperUser = true, roles = setOf("admin")))
            .sign(Algorithm.HMAC512(config.signingKey))

        val degraded = generator.extractUser(clientIp = "1.2.3.4", jwt = generator.verify(idLess))

        degraded.record.isAnonymous() shouldBe true
        degraded.permissions.isSuperUser shouldBe false
        degraded.permissions shouldBe UserPermissions.anonymous
    }
})
