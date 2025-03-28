package de.peekandpoke.ultra.common.remote

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class BuildUriSpec : StringSpec() {

    init {
        "buildUri with builder function should create correct URIs" {
            buildUri("https://example.com/api") {
                set("page", 1)
                set("size", 20)
            } shouldBe "https://example.com/api?page=1&size=20"
        }

        "buildUri with builder function should replace placeholders" {
            buildUri("https://example.com/api/users/{id}") {
                set("id", 123)
            } shouldBe "https://example.com/api/users/123"
        }

        "buildUri with builder function should handle mixed placeholders and query parameters" {
            buildUri("https://example.com/api/users/{userId}/posts/{postId}") {
                set("userId", "user123")
                set("postId", "post456")
                set("sort", "date")
                set("order", "desc")
            } shouldBe "https://example.com/api/users/user123/posts/post456?sort=date&order=desc"
        }

        "buildUri with builder function should handle different value types" {
            buildUri("https://example.com/api") {
                set("str", "string value")
                set("num", 42)
                set("decimal", 3.14)
                set("flag", true)
                setRaw("raw", "raw&value")
            } shouldBe "https://example.com/api?str=string%20value&num=42&decimal=3.14&flag=true&raw=raw&value"
        }

        "buildUri with builder function should ignore null and empty values" {
            buildUri("https://example.com/api") {
                set("valid", "data")
                set("empty", "")
                set("null", null as String?)
                set("nullNum", null as Number?)
                set("nullBool", null as Boolean?)
                setRaw("nullRaw", null)
            } shouldBe "https://example.com/api?valid=data"
        }

        "buildUri with builder function should properly encode special characters" {
            buildUri("https://example.com/search") {
                set("q", "hello world")
                set("filter", "price > 100")
                set("tags", "programming, coding")
            } shouldBe "https://example.com/search?q=hello%20world&filter=price%20%3E%20100&tags=programming%2C%20coding"
        }

        "buildUri with builder function should handle multiple parameters with the same name" {
            buildUri("https://example.com/{version}/api/{version}") {
                set("version", "v2")
            } shouldBe "https://example.com/v2/api/v2"
        }

        "buildUri with empty builder should not modify the URI" {
            buildUri("https://example.com/api") {
                // Empty builder, no parameters
            } shouldBe "https://example.com/api"
        }

        "buildUri with complex builder logic should work correctly" {
            buildUri("https://example.com/api/{resource}") {
                set("resource", "users")

                // Conditionally add parameters
                val includeDetails = true
                if (includeDetails) {
                    set("details", "full")
                }

                // Add multiple related parameters
                val page = 1
                val size = 25
                set("page", page)
                set("size", size)

                // Calculate parameter values
                val timestamp = 1624654321L
                set("t", timestamp)
            } shouldBe "https://example.com/api/users?details=full&page=1&size=25&t=1624654321"
        }
    }
}
