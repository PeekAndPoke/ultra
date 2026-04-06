package io.peekandpoke.funktor.rest.auth

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.ktor.http.*
import io.peekandpoke.funktor.core.broker.TypedRoute
import io.peekandpoke.funktor.core.broker.UriPattern
import io.peekandpoke.funktor.rest.ApiRoute
import io.peekandpoke.ultra.reflection.kType
import io.peekandpoke.ultra.remote.ApiAccessLevel
import io.peekandpoke.ultra.security.user.User
import io.peekandpoke.ultra.security.user.UserPermissions
import io.peekandpoke.ultra.security.user.UserRecord

class AuthenticatedRuleSpec : StringSpec({

    fun authenticatedRule(): AuthRule<Unit, Unit> {
        val builder = AuthRuleBuilder<Unit, Unit>(
            route = ApiRoute.Plain(
                method = HttpMethod.Get,
                route = TypedRoute.Plain(pattern = UriPattern("/test")),
                responseType = kType<Unit>(),
            )
        )
        return builder.authenticated()
    }

    fun routeWith(rule: AuthRule<Unit, Unit>): ApiRoute<Unit> {
        return ApiRoute.Plain(
            method = HttpMethod.Get,
            route = TypedRoute.Plain(pattern = UriPattern("/test")),
            responseType = kType<Unit>(),
            authRules = listOf(rule),
        )
    }

    "authenticated() grants for a logged-in user with roles" {
        val user = User(
            record = UserRecord(userId = "alice"),
            permissions = UserPermissions(roles = setOf("editor")),
        )
        val ctx = AuthRule.EstimateCtx(user = user)

        authenticatedRule().estimate(ctx) shouldBe ApiAccessLevel.Granted
    }

    "authenticated() grants for a logged-in user without roles" {
        val user = User(
            record = UserRecord(userId = "bob"),
            permissions = UserPermissions(),
        )
        val ctx = AuthRule.EstimateCtx(user = user)

        authenticatedRule().estimate(ctx) shouldBe ApiAccessLevel.Granted
    }

    "authenticated() grants for a super user" {
        val user = User(
            record = UserRecord(userId = "admin"),
            permissions = UserPermissions(isSuperUser = true),
        )
        val ctx = AuthRule.EstimateCtx(user = user)

        authenticatedRule().estimate(ctx) shouldBe ApiAccessLevel.Granted
    }

    "authenticated() denies for anonymous user" {
        val ctx = AuthRule.EstimateCtx(user = User.anonymous)

        authenticatedRule().estimate(ctx) shouldBe ApiAccessLevel.Denied
    }

    "authenticated() works with ApiRoute.estimateAccess(User)" {
        val route = routeWith(authenticatedRule())

        val authenticatedUser = User(
            record = UserRecord(userId = "alice"),
            permissions = UserPermissions(),
        )

        route.estimateAccess(authenticatedUser) shouldBe ApiAccessLevel.Granted
        route.estimateAccess(User.anonymous) shouldBe ApiAccessLevel.Denied
    }

    "authenticated() works with ApiRoute.estimateAccess(UserPermissions) - synthesized user is non-anonymous" {
        val route = routeWith(authenticatedRule())

        // estimateAccess(UserPermissions) synthesizes a non-anonymous user, so it should grant
        route.estimateAccess(UserPermissions()) shouldBe ApiAccessLevel.Granted
    }
})
