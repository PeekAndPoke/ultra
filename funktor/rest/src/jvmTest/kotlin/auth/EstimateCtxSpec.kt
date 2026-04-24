package io.peekandpoke.funktor.rest.auth

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.security.user.User
import io.peekandpoke.ultra.security.user.UserPermissions
import io.peekandpoke.ultra.security.user.UserRecord

class EstimateCtxSpec : StringSpec({

    "EstimateCtx exposes the full User" {
        val user = User(
            record = UserRecord.LoggedIn(userId = "alice", email = "alice@example.com"),
            permissions = UserPermissions(roles = setOf("editor")),
        )

        val ctx = AuthRule.EstimateCtx(user = user)

        ctx.user shouldBe user
    }

    "EstimateCtx.permissions exposes UserPermissions via delegate" {
        val perms = UserPermissions(roles = setOf("editor"), groups = setOf("staff"))
        val user = User(record = UserRecord.LoggedIn(userId = "bob"), permissions = perms)

        val ctx = AuthRule.EstimateCtx(user = user)

        ctx.permissions shouldBe perms
        ctx.permissions.roles shouldBe setOf("editor")
        ctx.permissions.groups shouldBe setOf("staff")
    }

    "isAuthenticated is true for a non-anonymous user" {
        val user = User(
            record = UserRecord.LoggedIn(userId = "alice"),
            permissions = UserPermissions(),
        )

        val ctx = AuthRule.EstimateCtx(user = user)

        ctx.isAuthenticated shouldBe true
    }

    "isAuthenticated is true even for authenticated users with no permissions" {
        // Key case: a logged-in user with no roles is NOT the same as anonymous
        val user = User(
            record = UserRecord.LoggedIn(userId = "no-roles-user"),
            permissions = UserPermissions(),  // empty — no roles, no groups, no perms
        )

        val ctx = AuthRule.EstimateCtx(user = user)

        ctx.isAuthenticated shouldBe true
    }

    "isAuthenticated is false for anonymous user" {
        val ctx = AuthRule.EstimateCtx(user = User.anonymous)

        ctx.isAuthenticated shouldBe false
    }

    "EstimateCtx.of(user) factory creates context" {
        val user = User(
            record = UserRecord.LoggedIn(userId = "carol"),
            permissions = UserPermissions(isSuperUser = true),
        )

        val ctx = AuthRule.EstimateCtx.of(user)

        ctx.user shouldBe user
        ctx.isAuthenticated shouldBe true
        ctx.permissions.isSuperUser shouldBe true
    }

    "EstimateCtx.of(User.anonymous) represents anonymous" {
        val ctx = AuthRule.EstimateCtx.of(User.anonymous)

        ctx.isAuthenticated shouldBe false
        ctx.user.isAnonymous() shouldBe true
    }
})
