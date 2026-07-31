package io.peekandpoke.funktor.insights

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.ktor.http.HttpMethod
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingNode
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

    /** Mounts [route] and returns the attribute bag the ktor route ended up carrying. */
    fun bagOn(route: ApiRoute.Plain<Unit>) = run {
        var found: io.peekandpoke.ultra.common.TypedAttributes? = null

        testApplication {
            routing {
                handle(route)

                // walk the tree the mount just built and read what it carries
                fun walk(node: Route) {
                    node.attributes.getOrNull(FunktorRouteAttributes)?.let { found = it }
                    (node as? RoutingNode)?.children?.forEach { walk(it) }
                }

                walk(this)
            }

            // testApplication is lazy — touch the app so `routing { }` actually runs
            client.config { }
        }

        found
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
