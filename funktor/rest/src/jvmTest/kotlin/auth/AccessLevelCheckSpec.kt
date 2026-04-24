package io.peekandpoke.funktor.rest.auth

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.remote.ApiAccessLevel
import io.peekandpoke.ultra.security.user.User
import io.peekandpoke.ultra.security.user.UserPermissions
import io.peekandpoke.ultra.security.user.UserRecord

class AccessLevelCheckSpec : StringSpec({

    val authenticatedUser = User(
        record = UserRecord.LoggedIn(userId = "alice"),
        permissions = UserPermissions(),
    )

    val anonymousUser = User.anonymous

    fun ruleReturning(level: ApiAccessLevel): AccessLevelCheck<Unit, Unit> {
        return AccessLevelCheck(description = "test") { level }
    }

    "estimate() returns Granted when rule returns Granted" {
        val rule = ruleReturning(ApiAccessLevel.Granted)
        rule.estimate(AuthRule.EstimateCtx(user = authenticatedUser)) shouldBe ApiAccessLevel.Granted
    }

    "estimate() returns Partial when rule returns Partial" {
        val rule = ruleReturning(ApiAccessLevel.Partial)
        rule.estimate(AuthRule.EstimateCtx(user = authenticatedUser)) shouldBe ApiAccessLevel.Partial
    }

    "estimate() returns Denied when rule returns Denied" {
        val rule = ruleReturning(ApiAccessLevel.Denied)
        rule.estimate(AuthRule.EstimateCtx(user = authenticatedUser)) shouldBe ApiAccessLevel.Denied
    }

    "Partial is treated as granted (not denied) for check semantics" {
        // AccessLevelCheck.check() uses: !rule(ctx).isDenied()
        // This means Partial is treated as access-allowed, unlike PermissionsCheck which is Boolean
        val rule = ruleReturning(ApiAccessLevel.Partial)
        val result = rule.estimate(AuthRule.EstimateCtx(user = authenticatedUser))
        result.isDenied() shouldBe false  // Partial is NOT denied
        result.isGranted() shouldBe false // But it's also NOT fully granted
    }

    "authenticated-only rule: estimate grants for authenticated user" {
        val rule = AccessLevelCheck<Unit, Unit>(description = "authenticated only") {
            if (isAuthenticated) ApiAccessLevel.Granted else ApiAccessLevel.Denied
        }
        rule.estimate(AuthRule.EstimateCtx(user = authenticatedUser)) shouldBe ApiAccessLevel.Granted
    }

    "authenticated-only rule: estimate denies for anonymous user" {
        val rule = AccessLevelCheck<Unit, Unit>(description = "authenticated only") {
            if (isAuthenticated) ApiAccessLevel.Granted else ApiAccessLevel.Denied
        }
        rule.estimate(AuthRule.EstimateCtx(user = anonymousUser)) shouldBe ApiAccessLevel.Denied
    }

    "rule can use permissions for fine-grained access levels" {
        val rule = AccessLevelCheck<Unit, Unit>(description = "role-based partial") {
            when {
                permissions.isSuperUser -> ApiAccessLevel.Granted
                permissions.hasRole("editor") -> ApiAccessLevel.Partial
                else -> ApiAccessLevel.Denied
            }
        }

        val superUser = User(
            record = UserRecord.LoggedIn(userId = "admin"),
            permissions = UserPermissions(isSuperUser = true),
        )
        val editor = User(
            record = UserRecord.LoggedIn(userId = "editor"),
            permissions = UserPermissions(roles = setOf("editor")),
        )
        val viewer = User(
            record = UserRecord.LoggedIn(userId = "viewer"),
            permissions = UserPermissions(),
        )

        rule.estimate(AuthRule.EstimateCtx(user = superUser)) shouldBe ApiAccessLevel.Granted
        rule.estimate(AuthRule.EstimateCtx(user = editor)) shouldBe ApiAccessLevel.Partial
        rule.estimate(AuthRule.EstimateCtx(user = viewer)) shouldBe ApiAccessLevel.Denied
    }
})
