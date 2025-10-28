package de.peekandpoke.kraft.routing

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class Route3Spec : StringSpec() {
    val renderer = Route.Renderer.Default

    init {
        "Route3 - simple pattern with three parameters" {
            val route = Route3("/user/{userId}/post/{postId}/comment/{commentId}")

            route.pattern shouldBe "/user/{userId}/post/{postId}/comment/{commentId}"
            val bound = route.bind("123", "456", "789")
            val rendered = renderer.render(bound)

            rendered shouldBe "/user/123/post/456/comment/789"
        }

        "Route3 - pattern with parameters at different positions" {
            val route = Route3("/{lang}/category/{categoryId}/product/{productId}")

            route.pattern shouldBe "/{lang}/category/{categoryId}/product/{productId}"
            val bound = route.bind("en", "electronics", "laptop-123")
            val rendered = renderer.render(bound)

            rendered shouldBe "/en/category/electronics/product/laptop-123"
        }

        "Route3 - pattern with consecutive parameters" {
            val route = Route3("/api/{version}/{service}/{endpoint}")

            route.pattern shouldBe "/api/{version}/{service}/{endpoint}"
            val bound = route.bind("v2", "users", "profile")
            val rendered = renderer.render(bound)

            rendered shouldBe "/api/v2/users/profile"
        }

        "Route3 - pattern with parameters scattered throughout" {
            val route = Route3("/organization/{orgId}/team/{teamId}/member/{memberId}/profile")

            route.pattern shouldBe "/organization/{orgId}/team/{teamId}/member/{memberId}/profile"
            val bound = route.bind(
                "acme",
                "dev-team",
                "john-doe"
            )
            val rendered = renderer.render(bound)

            rendered shouldBe "/organization/acme/team/dev-team/member/john-doe/profile"
        }

        "Route3 - pattern with named parameters" {
            val route = Route3("/company/{companyId}/department/{deptId}/employee/{empId}")

            route.pattern shouldBe "/company/{companyId}/department/{deptId}/employee/{empId}"
            val bound = route.bind(
                "acme-corp",
                "engineering",
                "john-smith"
            )
            val rendered = renderer.render(bound)

            rendered shouldBe "/company/acme-corp/department/engineering/employee/john-smith"
        }

        "Route3 - matching valid URI with parameter extraction" {
            val route = Route3("/user/{userId}/post/{postId}/comment/{commentId}")

            val match = route.match("/user/123/post/456/comment/789")
            match shouldNotBe null
            match?.routeParams shouldBe mapOf("userId" to "123", "postId" to "456", "commentId" to "789")
        }

        "Route3 - matching URI with complex parameter values" {
            val route = Route3("/store/{storeName}/category/{categorySlug}/item/{itemCode}")

            val match = route.match("/store/mega-mart/category/home-appliances/item/washing-machine-deluxe")
            match shouldNotBe null
            match?.routeParams shouldBe mapOf(
                "storeName" to "mega-mart",
                "categorySlug" to "home-appliances",
                "itemCode" to "washing-machine-deluxe"
            )
        }

        "Route3 - matching URI with numeric parameters" {
            val route = Route3("/year/{year}/month/{month}/day/{day}")

            val match = route.match("/year/2023/month/12/day/25")
            match shouldNotBe null
            match?.routeParams shouldBe mapOf("year" to "2023", "month" to "12", "day" to "25")
        }

        "Route3 - matching invalid URI structure with extra segments" {
            val route = Route3("/user/{userId}/post/{postId}/comment/{commentId}")

            val match = route.match("/user/123/post/456/comment/789/extra")
            match shouldBe null
        }

        "Route3 - matching URI with missing third parameter segment" {
            val route = Route3("/user/{userId}/post/{postId}/comment/{commentId}")

            val match = route.match("/user/123/post/456/comment/")
            match shouldNotBe null
            match?.routeParams shouldBe mapOf(
                "userId" to "123",
                "postId" to "456",
                "commentId" to "",
            )
        }

        "Route3 - matching URI with missing second parameter" {
            val route = Route3("/user/{userId}/post/{postId}/comment/{commentId}")

            val match = route.match("/user/123/post//comment/789")
            match shouldNotBe null
            match?.routeParams shouldBe mapOf(
                "userId" to "123",
                "postId" to "",
                "commentId" to "789",
            )
        }

        "Route3 - matching URI with missing first parameter" {
            val route = Route3("/user/{userId}/post/{postId}/comment/{commentId}")

            val match = route.match("/user//post/456/comment/789")
            match shouldNotBe null
            match?.routeParams shouldBe mapOf(
                "userId" to "",
                "postId" to "456",
                "commentId" to "789",
            )
        }

        "Route3 - matching completely different URI" {
            val route = Route3("/user/{userId}/post/{postId}/comment/{commentId}")

            val match = route.match("/product/123/review/456/rating/5")
            match shouldBe null
        }

        "Route3 - matching with wrong path structure" {
            val route = Route3("/api/{version}/users/{userId}/posts/{postId}")

            val match = route.match("/api/v1/admin/users/123/posts/456")
            match shouldBe null
        }

        "Route3 - pattern with no placeholders should throw error" {
            shouldThrow<IllegalStateException> {
                Route3("/static/path/here")
            }
        }

        "Route3 - pattern with one placeholder should throw error" {
            shouldThrow<IllegalStateException> {
                Route3("/user/{id}")
            }
        }

        "Route3 - pattern with two placeholders should throw error" {
            shouldThrow<IllegalStateException> {
                Route3("/user/{id}/post/{postId}")
            }
        }

        "Route3 - pattern with four placeholders should throw error" {
            shouldThrow<IllegalStateException> {
                Route3("/user/{id}/post/{postId}/comment/{commentId}/reply/{replyId}")
            }
        }

        "Route3 - pattern with zero placeholders should throw error with descriptive message" {
            val exception = shouldThrow<IllegalStateException> {
                Route3("/no/params/here")
            }
            exception.message shouldBe "The route '/no/params/here' has [0] route-params but should have [3]"
        }

        "Route3 - pattern with one placeholder should throw error with descriptive message" {
            val exception = shouldThrow<IllegalStateException> {
                Route3("/user/{id}")
            }
            exception.message shouldBe "The route '/user/{id}' has [1] route-params but should have [3]"
        }

        "Route3 - pattern with two placeholders should throw error with descriptive message" {
            val exception = shouldThrow<IllegalStateException> {
                Route3("/user/{id}/post/{postId}")
            }
            exception.message shouldBe "The route '/user/{id}/post/{postId}' has [2] route-params but should have [3]"
        }

        "Route3 - empty parameter values" {
            val route = Route3("/filter/{category}/{subcategory}/{brand}")

            renderer.render(route("", "", "")) shouldBe "/filter///"
        }

        "Route3 - mixed empty and non-empty parameter values" {
            val route = Route3("/search/{type}/{category}/{query}")

            renderer.render(route("product", "", "laptop")) shouldBe "/search/product//laptop"
            renderer.render(route("", "electronics", "")) shouldBe "/search//electronics/"
            renderer.render(route("advanced", "computers", "gaming")) shouldBe "/search/advanced/computers/gaming"
        }

        "Route3 - parameters with URL-like characters" {
            val route = Route3("/proxy/{source}/{target}/{fallback}")

            val bound = route(
                "https://api.example.com",
                "https://backup.example.com",
                "https://fallback.example.com"
            )
            val rendered = renderer.render(bound)

            rendered shouldBe "/proxy/https%3A%2F%2Fapi.example.com/https%3A%2F%2Fbackup.example.com/https%3A%2F%2Ffallback.example.com"
        }

        "Route3 - parameters with spaces and special characters" {
            val route = Route3("/book/{title}/{author}/{publisher}")

            val bound = route(
                "The Great Gatsby",
                "F. Scott Fitzgerald",
                "Charles Scribner's Sons"
            )
            val rendered = renderer.render(bound)

            rendered shouldBe "/book/The%20Great%20Gatsby/F.%20Scott%20Fitzgerald/Charles%20Scribner's%20Sons"
        }

        "Route3 - root level parameters" {
            val route = Route3("/{lang}/{country}/{region}")

            val bound = route("en", "us", "west")
            val rendered = renderer.render(bound)

            rendered shouldBe "/en/us/west"

            val match = route.match("/fr/ca/quebec")
            match shouldNotBe null
            match?.routeParams shouldBe mapOf("lang" to "fr", "country" to "ca", "region" to "quebec")
        }

        "Route3 - deeply nested parameters" {
            val route = Route3("/api/v1/organizations/{orgId}/projects/{projectId}/tasks/{taskId}/details")

            val bound = route(
                "org123",
                "proj456",
                "task789"
            )
            val rendered = renderer.render(bound)

            rendered shouldBe "/api/v1/organizations/org123/projects/proj456/tasks/task789/details"

            val match =
                route.match("/api/v1/organizations/acme-corp/projects/website-redesign/tasks/implement-login/details")
            match shouldNotBe null
            match?.routeParams shouldBe mapOf(
                "orgId" to "acme-corp",
                "projectId" to "website-redesign",
                "taskId" to "implement-login"
            )
        }

        // URL encoding/decoding round-trip tests
        "Route3 - round-trip with spaces - build then match should return original values" {
            val route = Route3("/search/{category}/{subcategory}/{query}")
            val originalValue1 = "home appliances"
            val originalValue2 = "kitchen equipment"
            val originalValue3 = "washing machine"

            val bound = route(originalValue1, originalValue2, originalValue3)
            val rendered = renderer.render(bound)
            val match = route.match(rendered)

            match shouldNotBe null
            match?.routeParams?.get("category") shouldBe originalValue1
            match?.routeParams?.get("subcategory") shouldBe originalValue2
            match?.routeParams?.get("query") shouldBe originalValue3
        }

        "Route3 - round-trip with special characters - build then match should return original values" {
            val route = Route3("/document/{title}/{author}/{tags}")
            val originalValue1 = "Q4 Report & Analysis"
            val originalValue2 = "John Smith & Associates"
            val originalValue3 = "finance, quarterly, 2023"

            val bound = route(originalValue1, originalValue2, originalValue3)
            val rendered = renderer.render(bound)
            val match = route.match(rendered)

            match shouldNotBe null
            match?.routeParams?.get("title") shouldBe originalValue1
            match?.routeParams?.get("author") shouldBe originalValue2
            match?.routeParams?.get("tags") shouldBe originalValue3
        }

        "Route3 - round-trip with URL characters - build then match should return original values" {
            val route = Route3("/redirect/{from}/{to}/{fallback}")
            val originalValue1 = "https://old.example.com/path?param=1"
            val originalValue2 = "https://new.example.com/newpath?param=2"
            val originalValue3 = "https://fallback.example.com/?error=redirect"

            val bound = route(originalValue1, originalValue2, originalValue3)
            val rendered = renderer.render(bound)
            val match = route.match(rendered)

            match shouldNotBe null
            match?.routeParams?.get("from") shouldBe originalValue1
            match?.routeParams?.get("to") shouldBe originalValue2
            match?.routeParams?.get("fallback") shouldBe originalValue3
        }

        "Route3 - round-trip with percent signs - build then match should return original values" {
            val route = Route3("/offer/{discount}/{code}/{description}")
            val originalValue1 = "25% OFF"
            val originalValue2 = "SAVE25%NOW"
            val originalValue3 = "Limited time 50% discount"

            val bound = route(originalValue1, originalValue2, originalValue3)
            val rendered = renderer.render(bound)
            val match = route.match(rendered)

            match shouldNotBe null
            match?.routeParams?.get("discount") shouldBe originalValue1
            match?.routeParams?.get("code") shouldBe originalValue2
            match?.routeParams?.get("description") shouldBe originalValue3
        }

        "Route3 - round-trip with plus signs - build then match should return original values" {
            val route = Route3("/math/{equation}/{operation}/{result}")
            val originalValue1 = "2+2"
            val originalValue2 = "addition+"
            val originalValue3 = "result=4+extra"

            val bound = route(originalValue1, originalValue2, originalValue3)
            val rendered = renderer.render(bound)
            val match = route.match(rendered)

            match shouldNotBe null
            match?.routeParams?.get("equation") shouldBe originalValue1
            match?.routeParams?.get("operation") shouldBe originalValue2
            match?.routeParams?.get("result") shouldBe originalValue3
        }

        "Route3 - round-trip with hash symbols - build then match should return original values" {
            val route = Route3("/social/{hashtag1}/{hashtag2}/{hashtag3}")
            val originalValue1 = "#awesome"
            val originalValue2 = "#coding"
            val originalValue3 = "#javascript"

            val bound = route(originalValue1, originalValue2, originalValue3)
            val rendered = renderer.render(bound)
            val match = route.match(rendered)

            match shouldNotBe null
            match?.routeParams?.get("hashtag1") shouldBe originalValue1
            match?.routeParams?.get("hashtag2") shouldBe originalValue2
            match?.routeParams?.get("hashtag3") shouldBe originalValue3
        }

        "Route3 - round-trip with unicode characters - build then match should return original values" {
            val route = Route3("/user/{firstName}/{middleName}/{lastName}")
            val originalValue1 = "José"
            val originalValue2 = "María"
            val originalValue3 = "García Fernández"

            val bound = route(originalValue1, originalValue2, originalValue3)
            val rendered = renderer.render(bound)
            val match = route.match(rendered)

            match shouldNotBe null
            match?.routeParams?.get("firstName") shouldBe originalValue1
            match?.routeParams?.get("middleName") shouldBe originalValue2
            match?.routeParams?.get("lastName") shouldBe originalValue3
        }

        "Route3 - round-trip with emojis - build then match should return original values" {
            val route = Route3("/reaction/{mood}/{activity}/{celebration}")
            val originalValue1 = "😊👍"
            val originalValue2 = "🎉🎈"
            val originalValue3 = "🥳🎊✨"

            val bound = route(originalValue1, originalValue2, originalValue3)
            val rendered = renderer.render(bound)
            val match = route.match(rendered)

            match shouldNotBe null
            match?.routeParams?.get("mood") shouldBe originalValue1
            match?.routeParams?.get("activity") shouldBe originalValue2
            match?.routeParams?.get("celebration") shouldBe originalValue3
        }

        "Route3 - round-trip with forward slashes - build then match should return original values" {
            val route = Route3("/path/{folder}/{subfolder}/{file}")
            val originalValue1 = "documents/2023"
            val originalValue2 = "reports/quarterly"
            val originalValue3 = "summary/final.pdf"

            val bound = route(originalValue1, originalValue2, originalValue3)
            val rendered = renderer.render(bound)
            val match = route.match(rendered)

            match shouldNotBe null
            match?.routeParams?.get("folder") shouldBe originalValue1
            match?.routeParams?.get("subfolder") shouldBe originalValue2
            match?.routeParams?.get("file") shouldBe originalValue3
        }

        "Route3 - round-trip with question marks - build then match should return original values" {
            val route = Route3("/qa/{question}/{followup}/{answer}")
            val originalValue1 = "What is love?"
            val originalValue2 = "Baby don't hurt me?"
            val originalValue3 = "No more?"

            val bound = route(originalValue1, originalValue2, originalValue3)
            val rendered = renderer.render(bound)
            val match = route.match(rendered)

            match shouldNotBe null
            match?.routeParams?.get("question") shouldBe originalValue1
            match?.routeParams?.get("followup") shouldBe originalValue2
            match?.routeParams?.get("answer") shouldBe originalValue3
        }

        "Route3 - matching URL-encoded parameters should decode correctly" {
            val route = Route3("/search/{category}/{subcategory}/{term}")

            val match = route.match("/search/home%20appliances/kitchen%20equipment/washing%20machine")
            match shouldNotBe null
            match?.routeParams?.get("category") shouldBe "home appliances"
            match?.routeParams?.get("subcategory") shouldBe "kitchen equipment"
            match?.routeParams?.get("term") shouldBe "washing machine"
        }

        "Route3 - matching parameters with encoded special characters should decode correctly" {
            val route = Route3("/document/{title}/{author}/{tags}")

            val match =
                route.match("/document/Q4%20Report%20%26%20Analysis/John%20Smith%20%26%20Associates/finance%2C%20quarterly%2C%202023")
            match shouldNotBe null
            match?.routeParams?.get("title") shouldBe "Q4 Report & Analysis"
            match?.routeParams?.get("author") shouldBe "John Smith & Associates"
            match?.routeParams?.get("tags") shouldBe "finance, quarterly, 2023"
        }

        "Route3 - matching parameters with encoded URLs should decode correctly" {
            val route = Route3("/redirect/{from}/{to}/{fallback}")

            val match =
                route.match("/redirect/https%3A%2F%2Fold.example.com%2Fpath%3Fparam%3D1/https%3A%2F%2Fnew.example.com%2Fnewpath%3Fparam%3D2/https%3A%2F%2Ffallback.example.com%2F%3Ferror%3Dredirect")
            match shouldNotBe null
            match?.routeParams?.get("from") shouldBe "https://old.example.com/path?param=1"
            match?.routeParams?.get("to") shouldBe "https://new.example.com/newpath?param=2"
            match?.routeParams?.get("fallback") shouldBe "https://fallback.example.com/?error=redirect"
        }

        "Route3 - round-trip with mixed simple and complex parameters" {
            val route = Route3("/user/{id}/{profile}/{settings}")
            val originalValue1 = "123"
            val originalValue2 = "John Doe & Associates"
            val originalValue3 = "theme=dark&lang=en"

            val bound = route(originalValue1, originalValue2, originalValue3)
            val rendered = renderer.render(bound)
            val match = route.match(rendered)

            match shouldNotBe null
            match?.routeParams?.get("id") shouldBe originalValue1
            match?.routeParams?.get("profile") shouldBe originalValue2
            match?.routeParams?.get("settings") shouldBe originalValue3
        }

        "Route3 - round-trip with all empty parameters" {
            val route = Route3("/filter/{type}/{category}/{brand}")
            val originalValue1 = ""
            val originalValue2 = ""
            val originalValue3 = ""

            val bound = route(originalValue1, originalValue2, originalValue3)
            val rendered = renderer.render(bound)
            val match = route.match(rendered)

            match shouldNotBe null
            match?.routeParams?.get("type") shouldBe originalValue1
            match?.routeParams?.get("category") shouldBe originalValue2
            match?.routeParams?.get("brand") shouldBe originalValue3
        }
    }
}
