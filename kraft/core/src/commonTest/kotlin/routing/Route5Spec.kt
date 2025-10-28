package de.peekandpoke.kraft.routing

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class Route5Spec : StringSpec() {
    val renderer = Route.Renderer.Default

    init {
        "Route5 - basic pattern with five parameters" {
            val route = Route5("/org/{orgId}/dept/{deptId}/team/{teamId}/project/{projectId}/task/{taskId}")

            route.pattern shouldBe "/org/{orgId}/dept/{deptId}/team/{teamId}/project/{projectId}/task/{taskId}"
            val bound = route.bind(
                "acme",
                "eng",
                "backend",
                "api",
                "auth"
            )
            val rendered = renderer.render(bound)

            rendered shouldBe "/org/acme/dept/eng/team/backend/project/api/task/auth"
        }

        "Route5 - consecutive parameters" {
            val route = Route5("/api/{version}/{service}/{resource}/{action}/{format}")

            val bound = route.bind("v2", "users", "profile", "update", "json")
            val rendered = renderer.render(bound)

            rendered shouldBe "/api/v2/users/profile/update/json"
        }

        "Route5 - matching valid URI with all parameter extraction" {
            val route = Route5("/store/{country}/{region}/{city}/{store}/{department}")

            val match = route.match("/store/usa/west/seattle/downtown/electronics")
            match shouldNotBe null
            match?.routeParams shouldBe mapOf(
                "country" to "usa",
                "region" to "west",
                "city" to "seattle",
                "store" to "downtown",
                "department" to "electronics"
            )
        }

        // Error condition tests
        "Route5 - pattern with zero placeholders should throw error" {
            val exception = shouldThrow<IllegalStateException> {
                Route5("/static/path/no/params/here")
            }
            exception.message shouldBe "The route '/static/path/no/params/here' has [0] route-params but should have [5]"
        }

        "Route5 - pattern with two placeholders should throw error" {
            val exception = shouldThrow<IllegalStateException> {
                Route5("/user/{id}/post/{postId}")
            }
            exception.message shouldBe "The route '/user/{id}/post/{postId}' has [2] route-params but should have [5]"
        }

        "Route5 - pattern with four placeholders should throw error" {
            val exception = shouldThrow<IllegalStateException> {
                Route5("/a/{p1}/b/{p2}/c/{p3}/d/{p4}")
            }
            exception.message shouldBe "The route '/a/{p1}/b/{p2}/c/{p3}/d/{p4}' has [4] route-params but should have [5]"
        }

        "Route5 - pattern with six placeholders should throw error" {
            val exception = shouldThrow<IllegalStateException> {
                Route5("/a/{p1}/b/{p2}/c/{p3}/d/{p4}/e/{p5}/f/{p6}")
            }
            exception.message shouldBe "The route '/a/{p1}/b/{p2}/c/{p3}/d/{p4}/e/{p5}/f/{p6}' has [6] route-params but should have [5]"
        }

        // Edge cases with empty parameters
        "Route5 - all empty parameters" {
            val route = Route5("/filter/{cat}/{subcat}/{brand}/{model}/{color}")

            renderer.render(route("", "", "", "", "")) shouldBe "/filter/////"
        }

        "Route5 - mixed empty and non-empty parameters" {
            val route = Route5("/search/{type}/{cat}/{query}/{sort}/{limit}")

            renderer.render(route("product", "", "laptop", "", "10")) shouldBe "/search/product//laptop//10"
            renderer.render(route("", "electronics", "", "price", "")) shouldBe "/search//electronics//price/"
        }

        "Route5 - only middle parameter non-empty" {
            val route = Route5("/path/{p1}/{p2}/{p3}/{p4}/{p5}")

            renderer.render(route("", "", "middle", "", "")) shouldBe "/path///middle//"
        }

        // Matching edge cases
        "Route5 - matching with missing parameter segments at different positions" {
            val route = Route5("/a/{p1}/b/{p2}/c/{p3}/d/{p4}/e/{p5}")

            route.match("/a/1/b/2/c/3/d/4/e/") shouldNotBe null
            route.match("/a/1/b/2/c//d/4/e/5") shouldNotBe null
            route.match("/a//b/2/c/3/d/4/e/5") shouldNotBe null
            route.match("/a/1/b/2/c/3/d//e/5") shouldNotBe null
            route.match("/a/1/b/2/c/3/d/4/e/") shouldNotBe null
            // The following should not match, because a part of the uri is missing
            route.match("/a/1/b/2/c/3/d/4") shouldBe null
        }

        "Route5 - matching with extra segments should fail" {
            val route = Route5("/a/{p1}/b/{p2}/c/{p3}/d/{p4}/e/{p5}")

            route.match("/a/1/b/2/c/3/d/4/e/5/extra") shouldBe null
            route.match("/a/1/b/2/c/3/d/4/e/5/f/6") shouldBe null
        }

        "Route5 - matching completely wrong structure" {
            val route =
                Route5("/user/{userId}/project/{projectId}/task/{taskId}/subtask/{subtaskId}/comment/{commentId}")

            route.match("/different/path/structure/completely/wrong") shouldBe null
        }

        // URL encoding round-trip edge cases
        "Route5 - round-trip with extreme special characters across all parameters" {
            val route = Route5("/test/{p1}/{p2}/{p3}/{p4}/{p5}")
            val values = listOf(
                "spaces & symbols!",
                "https://example.com/path?param=value",
                "100% guaranteed + more",
                "emoji test 🎉 #awesome",
                "forward/slash & question?"
            )

            val bound = route(values[0], values[1], values[2], values[3], values[4])
            val rendered = renderer.render(bound)
            val match = route.match(rendered)

            match shouldNotBe null
            match?.routeParams?.get("p1") shouldBe values[0]
            match?.routeParams?.get("p2") shouldBe values[1]
            match?.routeParams?.get("p3") shouldBe values[2]
            match?.routeParams?.get("p4") shouldBe values[3]
            match?.routeParams?.get("p5") shouldBe values[4]
        }

        "Route5 - round-trip with alternating empty and complex values" {
            val route = Route5("/alt/{p1}/{p2}/{p3}/{p4}/{p5}")
            val values = listOf(
                "",
                "complex value with & symbols",
                "",
                "José María García 🎯",
                ""
            )

            val bound = route(values[0], values[1], values[2], values[3], values[4])
            val rendered = renderer.render(bound)
            val match = route.match(rendered)

            match shouldNotBe null
            match?.routeParams?.get("p1") shouldBe values[0]
            match?.routeParams?.get("p2") shouldBe values[1]
            match?.routeParams?.get("p3") shouldBe values[2]
            match?.routeParams?.get("p4") shouldBe values[3]
            match?.routeParams?.get("p5") shouldBe values[4]
        }

        "Route5 - round-trip with maximum complexity parameters" {
            val route = Route5("/max/{simple}/{url}/{unicode}/{emoji}/{mixed}")
            val values = listOf(
                "simple",
                "https://api.example.com/v1/users?filter=active&sort=name",
                "Iñtërnâtiônàlizætiøn",
                "🚀💻🎯✨🌟",
                "Mixed: José's 100% café & more! 🎉"
            )

            val bound = route(values[0], values[1], values[2], values[3], values[4])
            val rendered = renderer.render(bound)
            val match = route.match(rendered)

            match shouldNotBe null
            match?.routeParams?.get("simple") shouldBe values[0]
            match?.routeParams?.get("url") shouldBe values[1]
            match?.routeParams?.get("unicode") shouldBe values[2]
            match?.routeParams?.get("emoji") shouldBe values[3]
            match?.routeParams?.get("mixed") shouldBe values[4]
        }

        // Direct encoded URI matching
        "Route5 - matching pre-encoded URI with complex special characters" {
            val route = Route5("/doc/{title}/{author}/{category}/{tags}/{metadata}")

            val match =
                route.match("/doc/My%20Document%20%26%20Analysis/Dr.%20José%20García/Tech%20%26%20Science/kotlin%2C%20testing%2C%20web/created%3D2023%26version%3D1.0")
            match shouldNotBe null
            match?.routeParams?.get("title") shouldBe "My Document & Analysis"
            match?.routeParams?.get("author") shouldBe "Dr. José García"
            match?.routeParams?.get("category") shouldBe "Tech & Science"
            match?.routeParams?.get("tags") shouldBe "kotlin, testing, web"
            match?.routeParams?.get("metadata") shouldBe "created=2023&version=1.0"
        }

        // Maximum nesting complexity
        "Route5 - deeply nested enterprise-level route" {
            val route =
                Route5("/api/v3/organizations/{orgId}/departments/{deptId}/teams/{teamId}/projects/{projectId}/epics/{epicId}/details")

            val bound = route(
                "multinational-corporation-holdings-ltd",
                "advanced-software-engineering-division",
                "backend-microservices-architecture-team",
                "next-generation-user-authentication-system",
                "implement-oauth2-with-jwt-tokens"
            )
            val rendered = renderer.render(bound)

            rendered shouldBe "/api/v3/organizations/multinational-corporation-holdings-ltd/departments/advanced-software-engineering-division/teams/backend-microservices-architecture-team/projects/next-generation-user-authentication-system/epics/implement-oauth2-with-jwt-tokens/details"

            val match =
                route.match("/api/v3/organizations/global-tech-solutions/departments/ai-research-development/teams/machine-learning-platform/projects/natural-language-processing/epics/sentiment-analysis-pipeline/details")
            match shouldNotBe null
            match?.routeParams shouldBe mapOf(
                "orgId" to "global-tech-solutions",
                "deptId" to "ai-research-development",
                "teamId" to "machine-learning-platform",
                "projectId" to "natural-language-processing",
                "epicId" to "sentiment-analysis-pipeline"
            )
        }

        // All root-level parameters
        "Route5 - all parameters at root level with complex values" {
            val route = Route5("/{p1}/{p2}/{p3}/{p4}/{p5}")

            val bound = route(
                "lang-en",
                "country-usa",
                "region-west",
                "city-seattle",
                "district-downtown"
            )
            val rendered = renderer.render(bound)

            rendered shouldBe "/lang-en/country-usa/region-west/city-seattle/district-downtown"

            val match = route.match("/fr/canada/quebec/montreal/old-port")
            match shouldNotBe null
            match?.routeParams shouldBe mapOf(
                "p1" to "fr",
                "p2" to "canada",
                "p3" to "quebec",
                "p4" to "montreal",
                "p5" to "old-port"
            )
        }

        // Stress test with very long parameter values
        "Route5 - stress test with extremely long parameter values" {
            val route = Route5("/long/{p1}/{p2}/{p3}/{p4}/{p5}")
            val longValue1 = "a".repeat(100)
            val longValue2 = "very-long-parameter-with-many-dashes-and-words-concatenated-together-to-test-url-encoding"
            val longValue3 = "Mixed content with spaces, symbols & unicode: José María García-Fernández 🎉✨"
            val longValue4 =
                "https://very-long-domain-name-for-testing-purposes.example.com/api/v1/users/profile/settings?theme=dark&language=english"
            val longValue5 =
                "Final parameter with everything: spaces, symbols & more, unicode José, emojis 🚀💻, and URLs https://test.com"

            val bound = route(longValue1, longValue2, longValue3, longValue4, longValue5)
            val rendered = renderer.render(bound)
            val match = route.match(rendered)

            match shouldNotBe null
            match?.routeParams?.get("p1") shouldBe longValue1
            match?.routeParams?.get("p2") shouldBe longValue2
            match?.routeParams?.get("p3") shouldBe longValue3
            match?.routeParams?.get("p4") shouldBe longValue4
            match?.routeParams?.get("p5") shouldBe longValue5
        }
    }
}
