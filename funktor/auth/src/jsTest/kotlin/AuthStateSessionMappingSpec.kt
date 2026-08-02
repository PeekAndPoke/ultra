package io.peekandpoke.funktor.auth

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.peekandpoke.funktor.auth.model.AuthOrgRef
import io.peekandpoke.funktor.auth.model.AuthRealmModel
import io.peekandpoke.funktor.auth.model.AuthSignInResponse
import io.peekandpoke.funktor.auth.model.PasswordPolicy
import io.peekandpoke.funktor.auth.model.RealmId
import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.security.user.OrgId
import io.peekandpoke.ultra.security.user.UserId
import io.peekandpoke.ultra.security.user.UserPermissions
import kotlinx.serialization.json.JsonObject

/**
 * The client maps the sign-in response; it never reads the token.
 *
 * This replaced `JwtClaimsSpec`, which tested a client-side JWT decoder that no longer exists. The decode
 * pulled permissions, `exp` and `sub` out of unverified claims — impossible once the token is an
 * `httpOnly` cookie, and never good, because those claims come from a blob the user can rewrite.
 *
 * The malformed-token rows are the point: if anyone reintroduces a decode, they fail.
 */
class AuthStateSessionMappingSpec : StringSpec({

    val realm = AuthRealmModel(
        id = RealmId("test-realm"),
        providers = emptyList(),
        passwordPolicy = PasswordPolicy.default,
    )

    val permissions = UserPermissions(
        isSuperUser = true,
        org = OrgId("orgs/o1"),
        roles = setOf("admin"),
    )

    val expiry = MpInstant.fromEpochSeconds(1_785_492_930L)

    fun successOf(session: AuthSignInResponse.Session) = AuthSignInResponse.Success(
        session = session,
        permissions = permissions,
        expiresAt = expiry,
        userId = UserId("users/u1"),
        realm = realm,
        user = JsonObject(emptyMap()),
        org = AuthOrgRef(id = OrgId("orgs/o1"), slug = "o1", name = "Org One"),
    )

    "a bearer session maps every field off the response" {

        val data = readSession(successOf(AuthSignInResponse.Session.Bearer("any.token.here")), user = "u")

        data.isLoggedIn shouldBe true
        data.permissions shouldBe permissions
        data.tokenExpires shouldBe expiry
        data.tokenUserId shouldBe UserId("users/u1")
        data.bearerToken shouldBe "any.token.here"
    }

    "a MALFORMED bearer token changes nothing — the mapping never parses it" {

        withClue("if this fails, someone reintroduced a client-side decode") {
            val data = readSession(successOf(AuthSignInResponse.Session.Bearer("not-a-jwt")), user = "u")

            data.permissions shouldBe permissions
            data.tokenExpires shouldBe expiry
            data.tokenUserId shouldBe UserId("users/u1")
        }
    }

    "an EMPTY bearer token changes nothing either" {

        val data = readSession(successOf(AuthSignInResponse.Session.Bearer("")), user = "u")

        data.permissions shouldBe permissions
        data.tokenExpires shouldBe expiry
        data.tokenUserId shouldBe UserId("users/u1")
    }

    "a cookie session carries no token, and everything else still arrives" {

        val data = readSession(successOf(AuthSignInResponse.Session.Cookie), user = "u")

        withClue("cookie mode has no token in the body — the browser holds it") {
            data.bearerToken.shouldBeNull()
        }

        data.isLoggedIn shouldBe true
        data.permissions shouldBe permissions
        data.tokenExpires shouldBe expiry
        data.tokenUserId shouldBe UserId("users/u1")
    }
})
