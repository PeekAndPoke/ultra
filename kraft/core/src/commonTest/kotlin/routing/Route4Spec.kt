package de.peekandpoke.kraft.routing

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class Route4Spec : StringSpec() {
    init {
        "Route4 - basic pattern with four parameters" {
            val route = Route4("/user/{userId}/project/{projectId}/task/{taskId}/comment/{commentId}")

            route.pattern shouldBe "/user/{userId}/project/{projectId}/task/{taskId}/comment/{commentId}"
            route.build("123", "456", "789", "101") shouldBe "#/user/123/project/456/task/789/comment/101"
        }

        "Route4 - consecutive parameters" {
            val route = Route4("/api/{version}/{service}/{resource}/{action}")

            route.build("v1", "users", "profile", "update") shouldBe "#/api/v1/users/profile/update"
        }

        "Route4 - matching valid URI with all parameter extraction" {
            val route = Route4("/org/{orgId}/dept/{deptId}/team/{teamId}/member/{memberId}")

            val match = route.match("/org/acme/dept/eng/team/backend/member/john")
            match shouldNotBe null
            match?.routeParams shouldBe mapOf(
                "orgId" to "acme",
                "deptId" to "eng",
                "teamId" to "backend",
                "memberId" to "john"
            )
        }

        "Route4 - build and buildUri should produce same result" {
            val route = Route4("/a/{p1}/b/{p2}/c/{p3}/d/{p4}")
            val params = listOf("val1", "val2", "val3", "val4")

            route.build(params[0], params[1], params[2], params[3]) shouldBe route.buildUri(
                params[0],
                params[1],
                params[2],
                params[3]
            )
        }

        // Error condition tests
        "Route4 - pattern with zero placeholders should throw error" {
            val exception = shouldThrow<IllegalStateException> {
                Route4("/static/path/no/params")
            }
            exception.message shouldBe "The route '/static/path/no/params' has [0] route-params but should have [4]"
        }

        "Route4 - pattern with one placeholder should throw error" {
            val exception = shouldThrow<IllegalStateException> {
                Route4("/user/{id}")
            }
            exception.message shouldBe "The route '/user/{id}' has [1] route-params but should have [4]"
        }

        "Route4 - pattern with three placeholders should throw error" {
            val exception = shouldThrow<IllegalStateException> {
                Route4("/user/{id}/post/{postId}/comment/{commentId}")
            }
            exception.message shouldBe "The route '/user/{id}/post/{postId}/comment/{commentId}' has [3] route-params but should have [4]"
        }

        "Route4 - pattern with five placeholders should throw error" {
            val exception = shouldThrow<IllegalStateException> {
                Route4("/a/{p1}/b/{p2}/c/{p3}/d/{p4}/e/{p5}")
            }
            exception.message shouldBe "The route '/a/{p1}/b/{p2}/c/{p3}/d/{p4}/e/{p5}' has [5] route-params but should have [4]"
        }

        // Edge cases with empty parameters
        "Route4 - all empty parameters" {
            val route = Route4("/filter/{cat}/{subcat}/{brand}/{model}")

            route.build("", "", "", "") shouldBe "#/filter////"
        }

        "Route4 - mixed empty and non-empty parameters" {
            val route = Route4("/search/{type}/{cat}/{query}/{sort}")

            route.build("product", "", "laptop", "") shouldBe "#/search/product//laptop/"
            route.build("", "electronics", "", "price") shouldBe "#/search//electronics//price"
        }

        // Matching edge cases
        "Route4 - matching with missing parameter segments" {
            val route = Route4("/a/{p1}/b/{p2}/c/{p3}/d/{p4}")

            route.match("/a/1/b/2/c/3/d/") shouldNotBe null
            route.match("/a/1/b/2/c//d/4") shouldNotBe null
            route.match("/a//b/2/c/3/d/4") shouldNotBe null
            route.match("/a/1/b/2/c/3/d/") shouldNotBe null
            // Missing uri part at the end
            route.match("/a/1/b/2/c/3") shouldBe null
        }

        "Route4 - matching with extra segments should fail" {
            val route = Route4("/a/{p1}/b/{p2}/c/{p3}/d/{p4}")

            route.match("/a/1/b/2/c/3/d/4/extra") shouldBe null
        }

        "Route4 - matching completely wrong structure" {
            val route = Route4("/user/{userId}/post/{postId}/comment/{commentId}/reply/{replyId}")

            route.match("/different/path/structure/here") shouldBe null
        }

        // URL encoding round-trip edge cases
        "Route4 - round-trip with extreme special characters" {
            val route = Route4("/test/{p1}/{p2}/{p3}/{p4}")
            val values = listOf(
                "value with spaces & symbols!",
                "https://example.com/path?param=value",
                "100% guaranteed + more",
                "emoji test 🎉 #awesome"
            )

            val builtUri = route.build(values[0], values[1], values[2], values[3])
            val match = route.match(builtUri.removePrefix("#"))

            match shouldNotBe null
            match?.routeParams?.get("p1") shouldBe values[0]
            match?.routeParams?.get("p2") shouldBe values[1]
            match?.routeParams?.get("p3") shouldBe values[2]
            match?.routeParams?.get("p4") shouldBe values[3]
        }

        "Route4 - round-trip with all empty values" {
            val route = Route4("/empty/{p1}/{p2}/{p3}/{p4}")
            val values = listOf("", "", "", "")

            val builtUri = route.build(values[0], values[1], values[2], values[3])
            val match = route.match(builtUri.removePrefix("#"))

            match shouldNotBe null
            match?.routeParams?.get("p1") shouldBe values[0]
            match?.routeParams?.get("p2") shouldBe values[1]
            match?.routeParams?.get("p3") shouldBe values[2]
            match?.routeParams?.get("p4") shouldBe values[3]
        }

        "Route4 - round-trip with mixed complexity parameters" {
            val route = Route4("/complex/{simple}/{encoded}/{unicode}/{emoji}")
            val values = listOf(
                "simple",
                "complex value with & symbols",
                "José María García",
                "🚀💻🎯✨"
            )

            val builtUri = route.build(values[0], values[1], values[2], values[3])
            val match = route.match(builtUri.removePrefix("#"))

            match shouldNotBe null
            match?.routeParams?.get("simple") shouldBe values[0]
            match?.routeParams?.get("encoded") shouldBe values[1]
            match?.routeParams?.get("unicode") shouldBe values[2]
            match?.routeParams?.get("emoji") shouldBe values[3]
        }

        // Direct encoded URI matching
        "Route4 - matching pre-encoded URI with special characters" {
            val route = Route4("/doc/{title}/{author}/{category}/{tags}")

            val match =
                route.match("/doc/My%20Document%20%26%20More/John%20Smith/Tech%20%26%20Science/kotlin%2C%20testing%2C%20web")
            match shouldNotBe null
            match?.routeParams?.get("title") shouldBe "My Document & More"
            match?.routeParams?.get("author") shouldBe "John Smith"
            match?.routeParams?.get("category") shouldBe "Tech & Science"
            match?.routeParams?.get("tags") shouldBe "kotlin, testing, web"
        }

        // Long parameter chains
        "Route4 - deeply nested route with long parameter values" {
            val route =
                Route4("/api/v2/organizations/{orgId}/departments/{deptId}/projects/{projectId}/tasks/{taskId}/details")

            route.build(
                "very-long-organization-name",
                "engineering-department",
                "website-redesign-project",
                "implement-user-authentication"
            ) shouldBe "#/api/v2/organizations/very-long-organization-name/departments/engineering-department/projects/website-redesign-project/tasks/implement-user-authentication/details"

            val match =
                route.match("/api/v2/organizations/acme-corporation-ltd/departments/software-engineering/projects/mobile-app-development/tasks/setup-ci-cd-pipeline/details")
            match shouldNotBe null
            match?.routeParams shouldBe mapOf(
                "orgId" to "acme-corporation-ltd",
                "deptId" to "software-engineering",
                "projectId" to "mobile-app-development",
                "taskId" to "setup-ci-cd-pipeline"
            )
        }

        // Root-level parameters
        "Route4 - all parameters at root level" {
            val route = Route4("/{p1}/{p2}/{p3}/{p4}")

            route.build("a", "b", "c", "d") shouldBe "#/a/b/c/d"

            val match = route.match("/en/us/admin/dashboard")
            match shouldNotBe null
            match?.routeParams shouldBe mapOf("p1" to "en", "p2" to "us", "p3" to "admin", "p4" to "dashboard")
        }
    }
}
