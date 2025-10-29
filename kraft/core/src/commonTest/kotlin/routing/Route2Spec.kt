@file:Suppress("detekt:all")

package de.peekandpoke.kraft.routing

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class Route2Spec : StringSpec() {
    val renderer = Route.Renderer.Default

    init {
        "Route2 - simple pattern with two parameters" {
            val route = Route2("/user/{userId}/post/{postId}")

            route.pattern shouldBe "/user/{userId}/post/{postId}"

            val bound = route.bind("123", "456")

            val rendered = renderer.render(bound)

            rendered shouldBe "/user/123/post/456"
        }

        "Route2 - pattern with parameters at different positions" {
            val route = Route2("/{category}/product/{id}")

            route.pattern shouldBe "/{category}/product/{id}"

            val bound = route.bind("electronics", "laptop-123")
            val rendered = renderer.render(bound)

            rendered shouldBe "/electronics/product/laptop-123"
        }

        "Route2 - pattern with parameters at the beginning and end" {
            val route = Route2("/{lang}/content/{slug}")

            route.pattern shouldBe "/{lang}/content/{slug}"

            val bound = route.bind("en", "welcome")
            val rendered = renderer.render(bound)

            rendered shouldBe "/en/content/welcome"
        }

        "Route2 - pattern with consecutive parameters" {
            val route = Route2("/api/{version}/{resource}")

            route.pattern shouldBe "/api/{version}/{resource}"

            val bound = route.bind("v1", "users")
            val rendered = renderer.render(bound)

            rendered shouldBe "/api/v1/users"
        }

        "Route2 - pattern with named parameters" {
            val route = Route2("/company/{companyId}/employee/{employeeId}")

            route.pattern shouldBe "/company/{companyId}/employee/{employeeId}"

            val bound = route.bind("acme-corp", "john-doe")
            val rendered = renderer.render(bound)

            rendered shouldBe "/company/acme-corp/employee/john-doe"
        }

        "Route2 - matching valid URI with parameter extraction" {
            val route = Route2("/user/{userId}/post/{postId}")

            val match = route.match("/user/123/post/456")
            match shouldNotBe null
            match?.routeParams shouldBe mapOf("userId" to "123", "postId" to "456")
        }

        "Route2 - matching URI with complex parameter values" {
            val route = Route2("/category/{categoryName}/item/{itemCode}")

            val match = route.match("/category/home-appliances/item/washing-machine-xl")
            match shouldNotBe null
            match?.routeParams shouldBe mapOf("categoryName" to "home-appliances", "itemCode" to "washing-machine-xl")
        }

        "Route2 - matching URI with numeric parameters" {
            val route = Route2("/year/{year}/month/{month}")

            val match = route.match("/year/2023/month/12")
            match shouldNotBe null
            match?.routeParams shouldBe mapOf("year" to "2023", "month" to "12")
        }

        "Route2 - matching invalid URI structure with extra segments" {
            val route = Route2("/user/{userId}/post/{postId}")

            val match = route.match("/user/123/post/456/extra")
            match shouldBe null
        }

        "Route2 - matching URI with missing parameter segments" {
            val route = Route2("/user/{userId}/post/{postId}")

            val match = route.match("/user/123/post/")
            match shouldNotBe null
            match?.routeParams shouldBe mapOf(
                "userId" to "123",
                "postId" to "",
            )
        }

        "Route2 - matching URI with missing second parameter completely" {
            val route = Route2("/user/{userId}/post/{postId}")

            val match = route.match("/user/123/post")
            match shouldBe null
        }

        "Route2 - matching URI with missing first parameter" {
            val route = Route2("/user/{userId}/post/{postId}")

            val match = route.match("/user//post/456")
            match shouldNotBe null
            match?.routeParams shouldBe mapOf(
                "userId" to "",
                "postId" to "456",
            )
        }

        "Route2 - matching completely different URI" {
            val route = Route2("/user/{userId}/post/{postId}")

            val match = route.match("/product/123/review/456")
            match shouldBe null
        }

        "Route2 - matching with wrong path structure" {
            val route = Route2("/api/{version}/users/{userId}")

            val match = route.match("/api/v1/admin/users/123")
            match shouldBe null
        }

        "Route2 - pattern with no placeholders should throw error" {
            shouldThrow<IllegalStateException> {
                Route2("/static/path")
            }
        }

        "Route2 - pattern with one placeholder should throw error" {
            shouldThrow<IllegalStateException> {
                Route2("/user/{id}")
            }
        }

        "Route2 - pattern with three placeholders should throw error" {
            shouldThrow<IllegalStateException> {
                Route2("/user/{id}/post/{postId}/comment/{commentId}")
            }
        }

        "Route2 - pattern with zero placeholders should throw error with descriptive message" {
            val exception = shouldThrow<IllegalStateException> {
                Route2("/no/params")
            }
            exception.message shouldBe "The route '/no/params' has [0] route-params but should have [2]"
        }

        "Route2 - pattern with one placeholder should throw error with descriptive message" {
            val exception = shouldThrow<IllegalStateException> {
                Route2("/user/{id}")
            }
            exception.message shouldBe "The route '/user/{id}' has [1] route-params but should have [2]"
        }

        "Route2 - pattern with three placeholders should throw error with descriptive message" {
            val exception = shouldThrow<IllegalStateException> {
                Route2("/user/{id}/post/{postId}/comment/{commentId}")
            }
            exception.message shouldBe "The route '/user/{id}/post/{postId}/comment/{commentId}' has [3] route-params but should have [2]"
        }

        "Route2 - empty parameter values" {
            val route = Route2("/search/{category}/{query}")

            renderer.render(route("", "")) shouldBe "/search//"
        }

        "Route2 - mixed empty and non-empty parameter values" {
            val route = Route2("/filter/{type}/{value}")

            renderer.render(route("category", "")) shouldBe "/filter/category/"
            renderer.render(route("", "electronics")) shouldBe "/filter//electronics"
        }

        "Route2 - parameters with URL-like characters" {
            val route = Route2("/proxy/{source}/{target}")

            val bound = route(
                "https://api.example.com",
                "https://backup.example.com"
            )
            val rendered = renderer.render(bound)

            rendered shouldBe "/proxy/https%3A%2F%2Fapi.example.com/https%3A%2F%2Fbackup.example.com"
        }

        "Route2 - parameters with spaces and special characters" {
            val route = Route2("/book/{title}/{author}")

            val bound = route(
                "The Great Gatsby",
                "F. Scott Fitzgerald"
            )
            val rendered = renderer.render(bound)

            rendered shouldBe "/book/The%20Great%20Gatsby/F.%20Scott%20Fitzgerald"
        }

        "Route2 - root level parameters" {
            val route = Route2("/{lang}/{country}")

            val bound = route("en", "us")
            val rendered = renderer.render(bound)

            rendered shouldBe "/en/us"

            val match = route.match("/fr/ca")
            match shouldNotBe null
            match?.routeParams shouldBe mapOf("lang" to "fr", "country" to "ca")
        }

        "Route2 - deeply nested parameters" {
            val route = Route2("/api/v1/organizations/{orgId}/projects/{projectId}/settings")

            val bound = route("org123", "proj456")
            val rendered = renderer.render(bound)

            rendered shouldBe "/api/v1/organizations/org123/projects/proj456/settings"

            val match = route.match("/api/v1/organizations/acme-corp/projects/website-redesign/settings")
            match shouldNotBe null
            match?.routeParams shouldBe mapOf("orgId" to "acme-corp", "projectId" to "website-redesign")
        }

        // URL encoding/decoding round-trip tests
        "Route2 - round-trip with spaces - build then match should return original values" {
            val route = Route2("/search/{category}/{query}")
            val originalValue1 = "home appliances"
            val originalValue2 = "washing machine"

            val bound = route(originalValue1, originalValue2)
            val rendered = renderer.render(bound)
            val match = route.match(rendered)

            match shouldNotBe null
            match?.routeParams?.get("category") shouldBe originalValue1
            match?.routeParams?.get("query") shouldBe originalValue2
        }

        "Route2 - round-trip with special characters - build then match should return original values" {
            val route = Route2("/document/{title}/{tags}")
            val originalValue1 = "My Report & Analysis"
            val originalValue2 = "finance, Q4, 2023"

            val bound = route(originalValue1, originalValue2)
            val rendered = renderer.render(bound)
            val match = route.match(rendered)

            match shouldNotBe null
            match?.routeParams?.get("title") shouldBe originalValue1
            match?.routeParams?.get("tags") shouldBe originalValue2
        }

        "Route2 - round-trip with URL characters - build then match should return original values" {
            val route = Route2("/redirect/{from}/{to}")
            val originalValue1 = "https://old.example.com/path?param=1"
            val originalValue2 = "https://new.example.com/newpath?param=2"

            val bound = route(originalValue1, originalValue2)
            val rendered = renderer.render(bound)
            val match = route.match(rendered)

            match shouldNotBe null
            match?.routeParams?.get("from") shouldBe originalValue1
            match?.routeParams?.get("to") shouldBe originalValue2
        }

        "Route2 - round-trip with percent signs - build then match should return original values" {
            val route = Route2("/offer/{discount}/{code}")
            val originalValue1 = "25% OFF"
            val originalValue2 = "SAVE25%NOW"

            val bound = route(originalValue1, originalValue2)
            val rendered = renderer.render(bound)
            val match = route.match(rendered)

            match shouldNotBe null
            match?.routeParams?.get("discount") shouldBe originalValue1
            match?.routeParams?.get("code") shouldBe originalValue2
        }

        "Route2 - round-trip with plus signs - build then match should return original values" {
            val route = Route2("/math/{equation}/{result}")
            val originalValue1 = "2+2"
            val originalValue2 = "a+b=4"

            val bound = route(originalValue1, originalValue2)
            val rendered = renderer.render(bound)
            val match = route.match(rendered)

            match shouldNotBe null
            match?.routeParams?.get("equation") shouldBe originalValue1
            match?.routeParams?.get("result") shouldBe originalValue2
        }

        "Route2 - round-trip with hash symbols - build then match should return original values" {
            val route = Route2("/social/{hashtag1}/{hashtag2}")
            val originalValue1 = "#awesome"
            val originalValue2 = "#coding"

            val bound = route(originalValue1, originalValue2)
            val rendered = renderer.render(bound)
            val match = route.match(rendered)

            match shouldNotBe null
            match?.routeParams?.get("hashtag1") shouldBe originalValue1
            match?.routeParams?.get("hashtag2") shouldBe originalValue2
        }

        "Route2 - round-trip with unicode characters - build then match should return original values" {
            val route = Route2("/user/{firstName}/{lastName}")
            val originalValue1 = "José"
            val originalValue2 = "María García"

            val bound = route(originalValue1, originalValue2)
            val rendered = renderer.render(bound)
            val match = route.match(rendered)

            match shouldNotBe null
            match?.routeParams?.get("firstName") shouldBe originalValue1
            match?.routeParams?.get("lastName") shouldBe originalValue2
        }

        "Route2 - round-trip with emojis - build then match should return original values" {
            val route = Route2("/reaction/{mood}/{activity}")
            val originalValue1 = "😊👍"
            val originalValue2 = "🎉🎈"

            val bound = route(originalValue1, originalValue2)
            val rendered = renderer.render(bound)
            val match = route.match(rendered)

            match shouldNotBe null
            match?.routeParams?.get("mood") shouldBe originalValue1
            match?.routeParams?.get("activity") shouldBe originalValue2
        }

        "Route2 - round-trip with forward slashes - build then match should return original values" {
            val route = Route2("/path/{folder}/{file}")
            val originalValue1 = "documents/2023"
            val originalValue2 = "report/final.pdf"

            val bound = route(originalValue1, originalValue2)
            val rendered = renderer.render(bound)
            val match = route.match(rendered)

            match shouldNotBe null
            match?.routeParams?.get("folder") shouldBe originalValue1
            match?.routeParams?.get("file") shouldBe originalValue2
        }

        "Route2 - round-trip with question marks - build then match should return original values" {
            val route = Route2("/qa/{question}/{answer}")
            val originalValue1 = "What is love?"
            val originalValue2 = "Baby don't hurt me?"

            val bound = route(originalValue1, originalValue2)
            val rendered = renderer.render(bound)
            val match = route.match(rendered)

            match shouldNotBe null
            match?.routeParams?.get("question") shouldBe originalValue1
            match?.routeParams?.get("answer") shouldBe originalValue2
        }

        "Route2 - matching URL-encoded parameters should decode correctly" {
            val route = Route2("/search/{category}/{term}")

            val match = route.match("/search/home%20appliances/washing%20machine")
            match shouldNotBe null
            match?.routeParams?.get("category") shouldBe "home appliances"
            match?.routeParams?.get("term") shouldBe "washing machine"
        }

        "Route2 - matching parameters with encoded special characters should decode correctly" {
            val route = Route2("/document/{title}/{tags}")

            val match = route.match("/document/My%20Report%20%26%20Analysis/finance%2C%20Q4%2C%202023")
            match shouldNotBe null
            match?.routeParams?.get("title") shouldBe "My Report & Analysis"
            match?.routeParams?.get("tags") shouldBe "finance, Q4, 2023"
        }

        "Route2 - matching parameters with encoded URLs should decode correctly" {
            val route = Route2("/redirect/{from}/{to}")

            val match =
                route.match("/redirect/https%3A%2F%2Fold.example.com%2Fpath%3Fparam%3D1/https%3A%2F%2Fnew.example.com%2Fnewpath%3Fparam%3D2")
            match shouldNotBe null
            match?.routeParams?.get("from") shouldBe "https://old.example.com/path?param=1"
            match?.routeParams?.get("to") shouldBe "https://new.example.com/newpath?param=2"
        }

        "Route2 - round-trip with mixed simple and complex parameters" {
            val route = Route2("/user/{id}/{profile}")
            val originalValue1 = "123"
            val originalValue2 = "John Doe & Associates"

            val bound = route(originalValue1, originalValue2)
            val rendered = renderer.render(bound)
            val match = route.match(rendered)

            match shouldNotBe null
            match?.routeParams?.get("id") shouldBe originalValue1
            match?.routeParams?.get("profile") shouldBe originalValue2
        }
    }
}
