package io.peekandpoke.funktor.insights.api

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.peekandpoke.funktor.rest.AuthChainBootCheck
import io.peekandpoke.ultra.remote.ApiAccessLevel
import io.peekandpoke.ultra.security.user.User
import io.peekandpoke.ultra.security.user.UserId
import io.peekandpoke.ultra.security.user.UserPermissions
import io.peekandpoke.ultra.security.user.UserRecord

/**
 * The gate sits on the route GROUP, so it must hold for every route in it — including any a later
 * change adds without thinking about auth. These checks need no running application; the proof that a
 * real HTTP request is refused lives in the e2e spec.
 *
 * Access is asserted through `estimateAccess`, the same evaluation the access matrix uses, rather than
 * by inspecting rule names — a route is gated if it *denies*, not if it mentions the right class.
 */
class InsightsApiRoutesSpec : StringSpec({

    val api = InsightsApi()

    val superUser = User(
        record = UserRecord.LoggedIn(userId = UserId("root")),
        permissions = UserPermissions(isSuperUser = true),
    )

    val ordinaryUser = User(
        record = UserRecord.LoggedIn(userId = UserId("alice")),
        permissions = UserPermissions(),
    )

    "both documented endpoints are mounted" {
        api.all.map { it.pattern.pattern } shouldContainExactlyInAnyOrder listOf(
            "${InsightsApi.base}/records",
            "${InsightsApi.base}/records/{bucket}/{file}",
        )
    }

    "a superuser is granted every route" {
        api.all.forEach { route ->
            withClue(route.pattern.pattern) {
                route.estimateAccess(superUser) shouldBe ApiAccessLevel.Granted
            }
        }
    }

    "an authenticated non-superuser is denied every route" {
        api.all.forEach { route ->
            withClue(route.pattern.pattern) {
                route.estimateAccess(ordinaryUser) shouldBe ApiAccessLevel.Denied
            }
        }
    }

    "an anonymous caller is denied every route" {
        api.all.forEach { route ->
            withClue(route.pattern.pattern) {
                route.estimateAccess(User.anonymous) shouldBe ApiAccessLevel.Denied
            }
        }
    }

    "every auth chain passes the start-up check" {
        // The same check the application runs while booting. A fail-open chain — one where a public
        // rule can satisfy the whole disjunction — surfaces here rather than at the first request.
        val check = AuthChainBootCheck()

        api.all.forEach { route ->
            withClue(route.pattern.pattern) {
                check.validate(route) shouldBe emptyList()
            }
        }
    }

    "the list limit is bounded, so one call cannot walk the whole depot" {
        InsightsApi.DEFAULT_LIMIT shouldBe 50
        (InsightsApi.DEFAULT_LIMIT <= InsightsApi.MAX_LIMIT) shouldBe true
    }
})
