package de.peekandpoke.kraft.routing

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class Route7Spec : StringSpec() {
    val renderer = Route.Renderer.Default

    init {
        "Route7 - basic pattern with seven parameters" {
            val route =
                Route7("/org/{orgId}/dept/{deptId}/team/{teamId}/project/{projectId}/task/{taskId}/subtask/{subtaskId}/item/{itemId}")

            route.pattern shouldBe "/org/{orgId}/dept/{deptId}/team/{teamId}/project/{projectId}/task/{taskId}/subtask/{subtaskId}/item/{itemId}"
            val bound = route.bind(
                "acme",
                "eng",
                "backend",
                "api",
                "auth",
                "validation",
                "unit-test"
            )
            val rendered = renderer.render(bound)

            rendered shouldBe "/org/acme/dept/eng/team/backend/project/api/task/auth/subtask/validation/item/unit-test"
        }

        "Route7 - consecutive parameters" {
            val route = Route7("/api/{version}/{service}/{resource}/{action}/{format}/{locale}/{timezone}")

            val bound = route.bind("v3", "users", "profile", "update", "json", "en", "utc")
            val rendered = renderer.render(bound)

            rendered shouldBe "/api/v3/users/profile/update/json/en/utc"
        }

        "Route7 - matching valid URI with all parameter extraction" {
            val route = Route7("/store/{country}/{region}/{city}/{store}/{department}/{section}/{product}")

            val match = route.match("/store/usa/west/seattle/downtown/electronics/phones/iphone")
            match shouldNotBe null
            match?.routeParams shouldBe mapOf(
                "country" to "usa",
                "region" to "west",
                "city" to "seattle",
                "store" to "downtown",
                "department" to "electronics",
                "section" to "phones",
                "product" to "iphone"
            )
        }

        "Route7 - pattern with incorrect parameter count should throw error" {
            val exception = shouldThrow<IllegalStateException> {
                Route7("/user/{id}/post/{postId}/{comment}")
            }
            exception.message shouldBe "The route '/user/{id}/post/{postId}/{comment}' has [3] route-params but should have [7]"
        }

        "Route7 - all empty parameters" {
            val route = Route7("/filter/{cat}/{subcat}/{brand}/{model}/{color}/{size}/{availability}")

            renderer.render(route("", "", "", "", "", "", "")) shouldBe "/filter///////"
        }

        "Route7 - round-trip with special characters" {
            val route = Route7("/test/{p1}/{p2}/{p3}/{p4}/{p5}/{p6}/{p7}")
            val values = listOf(
                "spaces & symbols!",
                "https://example.com",
                "100% guaranteed",
                "emoji test 🎉",
                "José María",
                "forward/slash",
                "final param"
            )

            val bound = route(values[0], values[1], values[2], values[3], values[4], values[5], values[6])
            val rendered = renderer.render(bound)
            val match = route.match(rendered)

            match shouldNotBe null
            match?.routeParams?.get("p1") shouldBe values[0]
            match?.routeParams?.get("p2") shouldBe values[1]
            match?.routeParams?.get("p3") shouldBe values[2]
            match?.routeParams?.get("p4") shouldBe values[3]
            match?.routeParams?.get("p5") shouldBe values[4]
            match?.routeParams?.get("p6") shouldBe values[5]
            match?.routeParams?.get("p7") shouldBe values[6]
        }

        "Route7 - matching with extra segments should fail" {
            val route = Route7("/a/{p1}/b/{p2}/c/{p3}/d/{p4}/e/{p5}/f/{p6}/g/{p7}")

            route.match("/a/1/b/2/c/3/d/4/e/5/f/6/g/7/extra") shouldBe null
        }
    }
}
