package io.peekandpoke.kraft.coretests.routing

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.kraft.routing.Route1
import io.peekandpoke.kraft.routing.Static
import io.peekandpoke.kraft.testing.TestBed
import io.peekandpoke.kraft.testing.awaitUri
import kotlinx.browser.window
import kotlinx.html.div

/** Route matching and percent-escapes (ULTRA-00273): a malformed one stays as written, nothing throws. */
class RouteMatchingSpec : StringSpec({

    // A hash left by a row must not reach the next spec
    afterTest { window.history.replaceState(null, "", window.location.pathname + window.location.search) }

    "well-formed escapes are decoded, in route params and in the query" {
        val match = Route1("/edit/{id}").match("/edit/a%20b?q=x%26y")!!

        match.routeParams["id"] shouldBe "a b"
        match.queryParams["q"] shouldBe "x&y"
    }

    "a malformed escape stays as it is written, in a route param and in the query" {
        val param = Route1("/edit/{id}").match("/edit/%")!!
        param.routeParams["id"] shouldBe "%"

        val query = Route1("/edit/{id}").match("/edit/1?q=%E0&r=ok")!!
        query.queryParams["q"] shouldBe "%E0"
        query.queryParams["r"] shouldBe "ok"
    }

    "a crafted link with a lone % is routed, not thrown on" {
        TestBed.preact(
            appSetup = {
                routing {
                    mount(Static("/a")) { div { } }
                    mount(Route1("/edit/{id}")) { div { } }
                }
            },
            view = { div { } },
        ) {
            router.navToUri("/a")
            router.awaitUri("/a")

            router.navToUri("/edit/1?q=%")
            router.awaitUri("/edit/1?q=%")

            router.current().matchedRoute.queryParams["q"] shouldBe "%"
        }
    }
})
