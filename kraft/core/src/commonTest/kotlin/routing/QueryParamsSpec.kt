@file:Suppress("detekt:all")

package de.peekandpoke.kraft.routing

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class QueryParamsSpec : StringSpec() {
    val renderer = Route.Renderer.Default

    init {
        // Static route with query parameters
        "Static route - rendering with empty query params" {
            val route = Static("/home")

            val bound = route.bind(routeParams = emptyMap(), queryParams = emptyMap())
            val rendered = renderer.render(bound)

            rendered shouldBe "/home"
        }

        "Static route - rendering with single query param" {
            val route = Static("/search")

            val bound = route.bind(routeParams = emptyMap(), queryParams = mapOf("q" to "kotlin"))
            val rendered = renderer.render(bound)

            rendered shouldBe "/search?q=kotlin"
        }

        "Static route - rendering with multiple query params" {
            val route = Static("/products")

            val bound = route.bind(
                routeParams = emptyMap(),
                queryParams = mapOf("category" to "electronics", "sort" to "price")
            )
            val rendered = renderer.render(bound)

            rendered shouldBe "/products?category=electronics&sort=price"
        }

        "Static route - rendering with query params containing spaces" {
            val route = Static("/search")

            val bound = route.bind(
                routeParams = emptyMap(),
                queryParams = mapOf("q" to "hello world", "type" to "full text")
            )
            val rendered = renderer.render(bound)

            rendered shouldBe "/search?q=hello%20world&type=full%20text"
        }

        "Static route - rendering with query params containing special characters" {
            val route = Static("/filter")

            val bound = route.bind(
                routeParams = emptyMap(),
                queryParams = mapOf("name" to "John & Jane", "category" to "R&D")
            )
            val rendered = renderer.render(bound)

            rendered shouldBe "/filter?name=John%20%26%20Jane&category=R%26D"
        }

        // Route1 with query parameters
        "Route1 - rendering with route param and query params" {
            val route = Route1("/user/{id}")

            val bound = route.bind(
                routeParams = mapOf("id" to "123"),
                queryParams = mapOf("tab" to "profile", "edit" to "true")
            )
            val rendered = renderer.render(bound)

            rendered shouldBe "/user/123?tab=profile&edit=true"
        }

        "Route1 - rendering with complex route param and query params" {
            val route = Route1("/product/{slug}")

            val bound = route.bind(
                routeParams = mapOf("slug" to "gaming-laptop"),
                queryParams = mapOf("color" to "silver", "warranty" to "2 years", "discount" to "10%")
            )
            val rendered = renderer.render(bound)

            rendered shouldBe "/product/gaming-laptop?color=silver&warranty=2%20years&discount=10%25"
        }

        // Route2 with query parameters
        "Route2 - rendering with route params and query params" {
            val route = Route2("/category/{category}/product/{id}")

            val bound = route.bind(
                routeParams = mapOf("category" to "electronics", "id" to "laptop-123"),
                queryParams = mapOf("view" to "detailed", "compare" to "true")
            )
            val rendered = renderer.render(bound)

            rendered shouldBe "/category/electronics/product/laptop-123?view=detailed&compare=true"
        }

        "Route2 - rendering with encoded route params and query params" {
            val route = Route2("/user/{name}/post/{title}")

            val bound = route.bind(
                routeParams = mapOf("name" to "john doe", "title" to "my first post"),
                queryParams = mapOf("draft" to "false", "tags" to "tech,programming")
            )
            val rendered = renderer.render(bound)

            rendered shouldBe "/user/john%20doe/post/my%20first%20post?draft=false&tags=tech%2Cprogramming"
        }

        // Route3 with query parameters
        "Route3 - rendering with route params and query params" {
            val route = Route3("/org/{orgId}/team/{teamId}/project/{projectId}")

            val bound = route.bind(
                routeParams = mapOf("orgId" to "acme", "teamId" to "dev", "projectId" to "web-app"),
                queryParams = mapOf("status" to "active", "priority" to "high", "assignee" to "john")
            )
            val rendered = renderer.render(bound)

            rendered shouldBe "/org/acme/team/dev/project/web-app?status=active&priority=high&assignee=john"
        }

        // Query parameter edge cases
        "Query params - empty values should be included" {
            val route = Static("/search")

            val bound = route.bind(
                routeParams = emptyMap(),
                queryParams = mapOf("q" to "", "filter" to "active")
            )
            val rendered = renderer.render(bound)

            rendered shouldBe "/search?q=&filter=active"
        }

        "Query params - null values should be filtered out" {
            val route = Static("/filter")

            val bound = route.bind(
                routeParams = emptyMap(),
                queryParams = mapOf("category" to "electronics", "brand" to null, "price" to "100")
            )
            val rendered = renderer.render(bound)

            rendered shouldBe "/filter?category=electronics&price=100"
        }

        "Query params - URL encoding special characters" {
            val route = Static("/api")

            val bound = route.bind(
                routeParams = emptyMap(),
                queryParams = mapOf(
                    "url" to "https://example.com/path?param=value",
                    "symbols" to "!@#$%^&*()",
                    "unicode" to "José María"
                )
            )
            val rendered = renderer.render(bound)

            rendered shouldBe "/api?url=https%3A%2F%2Fexample.com%2Fpath%3Fparam%3Dvalue&symbols=!%40%23%24%25%5E%26*()&unicode=Jos%C3%A9%20Mar%C3%ADa"
        }

        // Query parameter matching and extraction
        "Query params - parsing URI with query parameters" {
            val route = Static("/search")

            val match = route.match("/search?q=kotlin&type=tutorial")
            match shouldNotBe null
            match?.queryParams shouldBe mapOf("q" to "kotlin", "type" to "tutorial")
            match?.allParams shouldBe mapOf("q" to "kotlin", "type" to "tutorial")
        }

        "Query params - parsing URI with encoded query parameters" {
            val route = Static("/filter")

            val match = route.match("/filter?name=John%20Doe&category=R%26D&url=https%3A%2F%2Fexample.com")
            match shouldNotBe null
            match?.queryParams shouldBe mapOf(
                "name" to "John Doe",
                "category" to "R&D",
                "url" to "https://example.com"
            )
        }

        "Query params - parsing route with both route params and query params" {
            val route = Route2("/user/{userId}/post/{postId}")

            val match = route.match("/user/123/post/456?edit=true&version=2")
            match shouldNotBe null
            match?.routeParams shouldBe mapOf("userId" to "123", "postId" to "456")
            match?.queryParams shouldBe mapOf("edit" to "true", "version" to "2")
            match?.allParams shouldBe mapOf("userId" to "123", "postId" to "456", "edit" to "true", "version" to "2")
        }

        "Query params - parsing with empty query parameter values" {
            val route = Static("/search")

            val match = route.match("/search?q=&filter=active&sort=")
            match shouldNotBe null
            match?.queryParams shouldBe mapOf("q" to "", "filter" to "active", "sort" to "")
        }

        "Query params - parsing with duplicate parameter names (last one wins)" {
            val route = Static("/test")

            val match = route.match("/test?param=first&param=second&param=third")
            match shouldNotBe null
            match?.queryParams shouldBe mapOf("param" to "third")
        }

        "Query params - parsing complex query string with multiple encodings" {
            val route = Route1("/api/{version}")

            val match =
                route.match("/api/v1?callback=https%3A%2F%2Fapi.example.com%2Fcallback&data=%7B%22name%22%3A%22John%20Doe%22%7D&tags=kotlin%2Ctesting%2Capi")
            match shouldNotBe null
            match?.routeParams shouldBe mapOf("version" to "v1")
            match?.queryParams shouldBe mapOf(
                "callback" to "https://api.example.com/callback",
                "data" to "{\"name\":\"John Doe\"}",
                "tags" to "kotlin,testing,api"
            )
        }

        // Route.Match helper methods
        "Route.Match - withQueryParams should create new instance" {
            val route = Static("/test")
            val originalMatch = route.match("/test?original=value")!!

            val newMatch = originalMatch.withQueryParams(mapOf("new" to "param", "another" to "value"))

            newMatch.queryParams shouldBe mapOf("new" to "param", "another" to "value")
            originalMatch.queryParams shouldBe mapOf("original" to "value")
        }

        "Route.Match - withQueryParams with vararg should work" {
            val route = Static("/test")
            val originalMatch = route.match("/test")!!

            val newMatch = originalMatch.withQueryParams("param1" to "value1", "param2" to "value2")

            newMatch.queryParams shouldBe mapOf("param1" to "value1", "param2" to "value2")
        }

        "Route.Match - withQueryParams should filter out null values" {
            val route = Static("/test")
            val originalMatch = route.match("/test")!!

            val newMatch =
                originalMatch.withQueryParams(mapOf("keep" to "value", "remove" to null, "also_keep" to "another"))

            newMatch.queryParams shouldBe mapOf("keep" to "value", "also_keep" to "another")
        }

        "Route.Match - param method should return route param or default" {
            val route = Route2("/user/{userId}/post/{postId}")
            val match = route.match("/user/123/post/456?query=test")!!

            match.routeParam("userId") shouldBe "123"
            match.routeParam("postId") shouldBe "456"
            match.routeParam("nonexistent") shouldBe ""
            match.routeParam("nonexistent", "default") shouldBe "default"
        }

        "Route.Match - bracket operator should work like param" {
            val route = Route1("/user/{id}")
            val match = route.match("/user/123?tab=profile")!!

            match["id"] shouldBe "123"
            match["nonexistent"] shouldBe ""
        }

        "Route.Match - allParams should combine route and query params" {
            val route = Route2("/category/{cat}/item/{id}")
            val match = route.match("/category/electronics/item/laptop?sort=price&filter=available")!!

            match.allParams shouldBe mapOf(
                "cat" to "electronics",
                "id" to "laptop",
                "sort" to "price",
                "filter" to "available"
            )
        }

        // Edge cases and complex scenarios
        "Complex scenario - Route3 with encoded params and complex query params" {
            val route = Route3("/store/{country}/{city}/{branch}")

            val bound = route.bind(
                routeParams = mapOf("country" to "United States", "city" to "New York", "branch" to "5th Avenue"),
                queryParams = mapOf(
                    "department" to "Electronics & Computers",
                    "search" to "MacBook Pro 16\"",
                    "filters" to "color:silver,memory:32GB,price:<$3000",
                    "callback" to "https://analytics.example.com/track?event=search"
                )
            )

            val rendered = renderer.render(bound)

            val match = route.match(rendered)
            match shouldNotBe null
            match?.routeParams?.get("country") shouldBe "United States"
            match?.routeParams?.get("city") shouldBe "New York"
            match?.routeParams?.get("branch") shouldBe "5th Avenue"
            match?.queryParams?.get("department") shouldBe "Electronics & Computers"
            match?.queryParams?.get("search") shouldBe "MacBook Pro 16\""
            match?.queryParams?.get("filters") shouldBe "color:silver,memory:32GB,price:<$3000"
            match?.queryParams?.get("callback") shouldBe "https://analytics.example.com/track?event=search"
        }

        "URI parsing without query parameters should have empty queryParams" {
            val route = Route1("/user/{id}")

            val match = route.match("/user/123")
            match shouldNotBe null
            match?.queryParams shouldBe emptyMap()
            match?.allParams shouldBe mapOf("id" to "123")
        }

        "URI with only question mark should have empty queryParams" {
            val route = Static("/test")

            val match = route.match("/test?")
            match shouldNotBe null
            match?.queryParams shouldBe emptyMap()
        }

        "URI with malformed query parameters should handle gracefully" {
            val route = Static("/test")

            val match = route.match("/test?param1&param2=value&param3=")
            match shouldNotBe null
            // This tests the actual behavior - implementation may vary
            match?.queryParams?.keys?.contains("param2") shouldBe true
            match?.queryParams?.get("param2") shouldBe "value"
        }
    }
}
