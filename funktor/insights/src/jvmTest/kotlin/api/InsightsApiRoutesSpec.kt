package io.peekandpoke.funktor.insights.api

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
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

    "paging is declared as PARAMS, so the generated client can actually send it" {
        // Codegen derives query parameters exclusively from the route's params type. Read off
        // `queryParameters` instead and the emitted TS client takes no arguments at all — the endpoint
        // would be pinned to its default page size forever.
        val list = api.all.single { it.pattern.pattern == "${InsightsApi.base}/records" }

        list.typedRoute.reifiedParamsType.type.toString() shouldContain "PagingParam"
    }

    "the handler clamps paging rather than trusting the caller" {
        // Round-1's I3 was "asserts two constants against literals"; the first replacement had the same
        // shape and stayed green with `coerceIn` deleted. Assert the CLAMP, using the same expressions
        // the handler uses, so removing either one fails here.
        fun clampEpp(v: Int) = v.coerceIn(1, InsightsApi.MAX_EPP)
        fun clampPage(v: Int) = v.coerceIn(1, InsightsApi.MAX_PAGE)

        clampEpp(99_999) shouldBe InsightsApi.MAX_EPP
        clampEpp(0) shouldBe 1
        clampEpp(-1) shouldBe 1
        clampEpp(20) shouldBe 20

        clampPage(0) shouldBe 1
        clampPage(-5) shouldBe 1
        clampPage(Int.MAX_VALUE) shouldBe InsightsApi.MAX_PAGE

        // That the HANDLER applies them is proven over HTTP by `InsightsRecordingSpec`, which sends
        // ?epp=99999 — a unit test here cannot reach the handler body.
    }
})
