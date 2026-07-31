package io.peekandpoke.funktor.insights.api

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.funktor.rest.InsightsLevel
import io.peekandpoke.funktor.rest.ApiRoute
import io.peekandpoke.funktor.rest.docs.docs
import io.peekandpoke.funktor.rest.insights
import io.peekandpoke.funktor.rest.noInsights
import io.peekandpoke.funktor.core.broker.TypedRoute
import io.peekandpoke.funktor.core.broker.UriPattern
import io.ktor.http.HttpMethod
import io.peekandpoke.ultra.reflection.kType

/**
 * The insights API must not record itself: a call to it would otherwise write a record whose slices
 * describe the superuser who was *reading* insights, and every read would append another.
 *
 * This replaces `InsightsExclusionSpec`, which asserted a substring match over `call.request.uri`. That
 * check was defeatable two ways — percent-encoding a path character, and putting the API's base path in
 * a query string — because it read a value the client controls. The level is declared on the route, so
 * neither bypass has anywhere to act.
 */
class InsightsLevelSpec : StringSpec({

    val api = InsightsApi()

    fun plainRoute() = ApiRoute.Plain<Unit>(
        method = HttpMethod.Get,
        route = TypedRoute.Plain(pattern = UriPattern("/x")),
        responseType = kType<Unit>(),
    )

    "every insights route opts out of recording" {
        api.all.size shouldBe 2

        api.all.forEach { route ->
            withClue(route.pattern.pattern) {
                route.insights.level shouldBe InsightsLevel.OFF
            }
        }
    }

    "a route with no attribute records in FULL and keeps its query params" {
        // The default has to be FULL, not OFF: every route in every other module carries no attribute
        // at all, and they must keep being recorded.
        plainRoute().insights.level shouldBe InsightsLevel.FULL
        plainRoute().insights.dropQueryParams shouldBe false
    }

    "the builder sets each level, and the options survive further chaining" {
        plainRoute().noInsights().insights.level shouldBe InsightsLevel.OFF
        plainRoute().insights { brief() }.insights.level shouldBe InsightsLevel.BRIEF
        plainRoute().insights { full() }.insights.level shouldBe InsightsLevel.FULL

        // a builder applied AFTER must not drop the bag — `docs {}` copies attributes forward
        plainRoute().noInsights().docs { name = "x" }.insights.level shouldBe InsightsLevel.OFF
        plainRoute().docs { name = "x" }.noInsights().insights.level shouldBe InsightsLevel.OFF
    }

    "dropQueryParams is independent of the level" {
        val route = plainRoute().insights { brief(); dropQueryParams() }

        route.insights.level shouldBe InsightsLevel.BRIEF
        route.insights.dropQueryParams shouldBe true

        // and it is off unless asked for — redaction by name is the default, dropping is the escalation
        plainRoute().insights { brief() }.insights.dropQueryParams shouldBe false
    }
})
