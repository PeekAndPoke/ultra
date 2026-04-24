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

class ApiRouteEstimateAccessSpec : StringSpec({

    fun routeWithRules(vararg rules: AuthRule<Unit, Unit>): ApiRoute<Unit> {
        return ApiRoute.Plain(
            method = HttpMethod.Get,
            route = TypedRoute.Plain(pattern = UriPattern("/test")),
            responseType = kType<Unit>(),
            authRules = rules.toList(),
        )
    }

    "estimateAccess(UserPermissions) synthesizes a non-anonymous User" {
        // A rule that grants only when the user is authenticated
        val authenticatedOnly: AuthRule<Unit, Unit> = AuthRule.forCall(
            description = "authenticated only",
            estimateFn = { if (isAuthenticated) ApiAccessLevel.Granted else ApiAccessLevel.Denied },
        ) { !user.isAnonymous() }

        val route = routeWithRules(authenticatedOnly)

        // An empty permissions set — synthesized user must be non-anonymous
        route.estimateAccess(UserPermissions()) shouldBe ApiAccessLevel.Granted

        // With roles — still granted
        route.estimateAccess(UserPermissions(roles = setOf("editor"))) shouldBe ApiAccessLevel.Granted

        // SuperUser — still granted
        route.estimateAccess(UserPermissions(isSuperUser = true)) shouldBe ApiAccessLevel.Granted
    }

    "estimateAccess(User) with anonymous user denies authenticated-only rules" {
        val authenticatedOnly: AuthRule<Unit, Unit> = AuthRule.forCall(
            description = "authenticated only",
            estimateFn = { if (isAuthenticated) ApiAccessLevel.Granted else ApiAccessLevel.Denied },
        ) { !user.isAnonymous() }

        val route = routeWithRules(authenticatedOnly)

        route.estimateAccess(User.anonymous) shouldBe ApiAccessLevel.Denied
    }

    "estimateAccess(User) with named user grants authenticated-only rules" {
        val authenticatedOnly: AuthRule<Unit, Unit> = AuthRule.forCall(
            description = "authenticated only",
            estimateFn = { if (isAuthenticated) ApiAccessLevel.Granted else ApiAccessLevel.Denied },
        ) { !user.isAnonymous() }

        val route = routeWithRules(authenticatedOnly)
        val alice = User(
            record = UserRecord.LoggedIn(userId = "alice"),
            permissions = UserPermissions(),
        )

        route.estimateAccess(alice) shouldBe ApiAccessLevel.Granted
    }

    "estimateAccess folds multiple rules with most-restrictive wins" {
        // Rule 1: granted for all
        val ruleA: AuthRule<Unit, Unit> = AuthRule.forCall(
            description = "always granted",
            estimateFn = { ApiAccessLevel.Granted },
        ) { true }

        // Rule 2: denies for non-superuser
        val ruleB: AuthRule<Unit, Unit> = AuthRule.forCall(
            description = "superuser only",
            estimateFn = {
                if (permissions.isSuperUser) ApiAccessLevel.Granted else ApiAccessLevel.Denied
            },
        ) { permissions.isSuperUser }

        val route = routeWithRules(ruleA, ruleB)

        // Regular user → denied (most restrictive wins)
        route.estimateAccess(UserPermissions()) shouldBe ApiAccessLevel.Denied

        // SuperUser → granted
        route.estimateAccess(UserPermissions(isSuperUser = true)) shouldBe ApiAccessLevel.Granted
    }

    "route with no auth rules defaults to Granted" {
        val route = routeWithRules()

        route.estimateAccess(UserPermissions()) shouldBe ApiAccessLevel.Granted
        route.estimateAccess(User.anonymous) shouldBe ApiAccessLevel.Granted
    }
})
