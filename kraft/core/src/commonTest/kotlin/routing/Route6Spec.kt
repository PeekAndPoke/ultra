package de.peekandpoke.kraft.routing

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class Route6Spec : StringSpec() {
    val renderer = Route.Renderer.Default

    init {
        "Route6 - basic pattern with six parameters" {
            val route =
                Route6("/org/{orgId}/dept/{deptId}/team/{teamId}/project/{projectId}/task/{taskId}/subtask/{subtaskId}")

            route.pattern shouldBe "/org/{orgId}/dept/{deptId}/team/{teamId}/project/{projectId}/task/{taskId}/subtask/{subtaskId}"
            val bound = route.bind(
                "acme",
                "eng",
                "backend",
                "api",
                "auth",
                "validation"
            )
            val rendered = renderer.render(bound)

            rendered shouldBe "/org/acme/dept/eng/team/backend/project/api/task/auth/subtask/validation"
        }

        "Route6 - consecutive parameters" {
            val route = Route6("/api/{version}/{service}/{resource}/{action}/{format}/{locale}")

            val bound = route.bind("v2", "users", "profile", "update", "json", "en")
            val rendered = renderer.render(bound)

            rendered shouldBe "/api/v2/users/profile/update/json/en"
        }

        "Route6 - matching valid URI with all parameter extraction" {
            val route = Route6("/store/{country}/{region}/{city}/{store}/{department}/{section}")

            val match = route.match("/store/usa/west/seattle/downtown/electronics/phones")
            match shouldNotBe null
            match?.routeParams shouldBe mapOf(
                "country" to "usa",
                "region" to "west",
                "city" to "seattle",
                "store" to "downtown",
                "department" to "electronics",
                "section" to "phones"
            )
        }

        "Route6 - pattern with incorrect parameter count should throw error" {
            val exception = shouldThrow<IllegalStateException> {
                Route6("/user/{id}/post/{postId}")
            }
            exception.message shouldBe "The route '/user/{id}/post/{postId}' has [2] route-params but should have [6]"
        }

        "Route6 - all empty parameters" {
            val route = Route6("/filter/{cat}/{subcat}/{brand}/{model}/{color}/{size}")

            renderer.render(route("", "", "", "", "", "")) shouldBe "/filter//////"
        }

        "Route6 - round-trip with special characters" {
            val route = Route6("/test/{p1}/{p2}/{p3}/{p4}/{p5}/{p6}")
            val values = listOf(
                "spaces & symbols!",
                "https://example.com",
                "100% guaranteed",
                "emoji test 🎉",
                "José María",
                "forward/slash"
            )

            val bound = route(values[0], values[1], values[2], values[3], values[4], values[5])
            val rendered = renderer.render(bound)
            val match = route.match(rendered)

            match shouldNotBe null
            match?.routeParams?.get("p1") shouldBe values[0]
            match?.routeParams?.get("p2") shouldBe values[1]
            match?.routeParams?.get("p3") shouldBe values[2]
            match?.routeParams?.get("p4") shouldBe values[3]
            match?.routeParams?.get("p5") shouldBe values[4]
            match?.routeParams?.get("p6") shouldBe values[5]
        }

        "Route6 - matching with extra segments should fail" {
            val route = Route6("/a/{p1}/b/{p2}/c/{p3}/d/{p4}/e/{p5}/f/{p6}")

            route.match("/a/1/b/2/c/3/d/4/e/5/f/6/extra") shouldBe null
        }
    }
}
