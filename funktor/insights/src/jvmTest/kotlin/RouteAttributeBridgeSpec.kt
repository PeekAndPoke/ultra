package io.peekandpoke.funktor.insights

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.ktor.http.HttpMethod
import io.ktor.server.routing.Route
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.peekandpoke.funktor.core.broker.TypedRoute
import io.peekandpoke.funktor.core.broker.UriPattern
import io.peekandpoke.funktor.rest.ApiRoute
import io.peekandpoke.funktor.rest.FunktorRouteAttributes
import io.peekandpoke.funktor.rest.InsightsLevel
import io.peekandpoke.funktor.rest.InsightsOptionsKey
import io.peekandpoke.funktor.rest.handle
import io.peekandpoke.funktor.rest.insights
import io.peekandpoke.funktor.rest.noInsights
import io.peekandpoke.ultra.common.TypedAttributes
import io.peekandpoke.ultra.reflection.kType

/**
 * Proves an `ApiRoute`'s attributes actually land on the ktor route it is mounted as.
 *
 * `InsightsLevelSpec` covers the build-time half — `insights { }` sets the attribute on the `ApiRoute`.
 * This covers the crossing: `Route.handle` copies the bag onto the ktor route, which is the only reason
 * `insightsOptions()` can find anything at request time.
 *
 * Written because deleting that one line broke **nothing**. Every suite stayed green while the recorder
 * silently fell back to FULL — which would put the insights API's own responses, containing the reading
 * superuser's headers, straight back into the depot.
 *
 * Asserted against the mounted route tree rather than through a request: driving a funktor handler needs
 * the kontainer and a `UserProvider` for phase-1 auth, and none of that is what broke.
 */
class RouteAttributeBridgeSpec : StringSpec({

    fun plainRoute() = ApiRoute.Plain<Unit>(
        method = HttpMethod.Get,
        route = TypedRoute.Plain(pattern = UriPattern("/probe")),
        responseType = kType<Unit>(),
    )

    /**
     * Mounts [route] and returns the bag carried by **the node `handle` returned** — which is the node
     * `insightsOptions()` reads at request time (`RoutingPipelineCall.route`, the resolved leaf).
     *
     * Deliberately NOT a walk of the whole subtree. The first version of this spec did walk, and so it
     * stayed green under the mutation that matters most: putting the bag on the PARENT instead of the
     * mounted node. The walk still found it, all three tests passed, and `insightsOptions()` fell back
     * to FULL at request time — the exact regression this file exists to catch.
     */
    fun bagOn(route: ApiRoute.Plain<Unit>): TypedAttributes? {
        var mounted: Route? = null

        testApplication {
            routing {
                mounted = handle(route)
            }

            // testApplication is lazy — touch the app so `routing { }` actually runs
            client.config { }
        }

        return mounted?.attributes?.getOrNull(FunktorRouteAttributes)
    }

    "the attribute bag crosses onto the mounted ktor route" {
        val bag = bagOn(plainRoute().noInsights()).shouldNotBeNull()

        bag[InsightsOptionsKey]?.level shouldBe InsightsLevel.OFF
    }

    "every option survives the crossing, not just the level" {
        val bag = bagOn(plainRoute().insights { brief(); dropQueryParams() }).shouldNotBeNull()

        bag[InsightsOptionsKey]?.level shouldBe InsightsLevel.BRIEF
        bag[InsightsOptionsKey]?.dropQueryParams shouldBe true
    }

    "a route with no options still gets a bag, so the reader distinguishes empty from missing" {
        val bag = bagOn(plainRoute()).shouldNotBeNull()

        bag[InsightsOptionsKey] shouldBe null
    }
})
