package routing

import de.peekandpoke.kraft.routing.Route1
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

class Route1Spec : StringSpec() {
    init {
        "Route1 - simple pattern with one parameter" {
            val route = Route1("/user/{id}")

            route.pattern shouldBe "/user/{id}"
            route.build("123") shouldBe "#/user/123"
        }

        "Route1 - pattern with named parameter" {
            val route = Route1("/product/{productId}")

            route.pattern shouldBe "/product/{productId}"
            route.build("abc-123") shouldBe "#/product/abc-123"
        }

        "Route1 - pattern with parameter at the beginning" {
            val route = Route1("/{category}/list")

            route.pattern shouldBe "/{category}/list"
            route.build("electronics") shouldBe "#/electronics/list"
        }

        "Route1 - pattern with parameter at the end" {
            val route = Route1("/api/v1/{endpoint}")

            route.pattern shouldBe "/api/v1/{endpoint}"
            route.build("users") shouldBe "#/api/v1/users"
        }

        "Route1 - pattern with parameter containing special characters" {
            val route = Route1("/file/{filename}")

            route.build("test-file_v1.2.txt") shouldBe "#/file/test-file_v1.2.txt"
        }

        "Route1 - matching valid URI with parameter extraction" {
            val route = Route1("/user/{id}")

            val match = route.match("/user/456")
            match shouldNotBe null
            match?.routeParams shouldBe mapOf("id" to "456")
        }

        "Route1 - matching URI with complex parameter value" {
            val route = Route1("/category/{name}")

            val match = route.match("/category/home-appliances")
            match shouldNotBe null
            match?.routeParams shouldBe mapOf("name" to "home-appliances")
        }

        "Route1 - matching URI with numeric parameter" {
            val route = Route1("/order/{orderId}")

            val match = route.match("/order/12345")
            match shouldNotBe null
            match?.routeParams shouldBe mapOf("orderId" to "12345")
        }

        "Route1 - matching invalid URI structure with extra segments" {
            val route = Route1("/user/{id}")

            val match = route.match("/user/123/extra")
            match shouldBe null
        }

        "Route1 - matching URI with missing parameter segment" {
            val route = Route1("/user/{id}")

            val match = route.match("/user/")
            match shouldNotBe null
            match?.routeParams shouldBe mapOf("id" to "")
        }

        "Route1 - matching URI with missing parameter completely" {
            val route = Route1("/user/{id}")

            val match = route.match("/user")
            match shouldBe null
        }

        "Route1 - matching completely different URI" {
            val route = Route1("/user/{id}")

            val match = route.match("/product/123")
            match shouldBe null
        }

        "Route1 - matching with wrong path structure" {
            val route = Route1("/api/{version}/data")

            val match = route.match("/api/v1/users/data")
            match shouldBe null
        }

        "Route1 - buildUri with vararg should work" {
            val route = Route1("/item/{id}")

            route.buildUri("xyz-789") shouldBe "#/item/xyz-789"
        }

        "Route1 - build and buildUri should produce same result" {
            val route = Route1("/document/{docId}")
            val paramValue = "doc-456"

            route.build(paramValue) shouldBe route.buildUri(paramValue)
        }

        "Route1 - pattern with no placeholders should throw error" {
            shouldThrow<IllegalStateException> {
                Route1("/static/path")
            }
        }

        "Route1 - pattern with multiple placeholders should throw error" {
            shouldThrow<IllegalStateException> {
                Route1("/user/{id}/post/{postId}")
            }
        }

        "Route1 - pattern with zero placeholders should throw error with descriptive message" {
            val exception = shouldThrow<IllegalStateException> {
                Route1("/no/params")
            }
            exception.message shouldBe "The route '/no/params' has [0] route-params but should have [1]"
        }

        "Route1 - pattern with two placeholders should throw error with descriptive message" {
            val exception = shouldThrow<IllegalStateException> {
                Route1("/user/{id}/comment/{commentId}")
            }
            exception.message shouldBe "The route '/user/{id}/comment/{commentId}' has [2] route-params but should have [1]"
        }

        "Route1 - empty parameter value" {
            val route = Route1("/search/{query}")

            route.build("") shouldBe "#/search/"
        }

        "Route1 - parameter with URL-like characters" {
            val route = Route1("/redirect/{url}")

            route.build("https://example.com") shouldBe "#/redirect/https%3A%2F%2Fexample.com"
        }

        "Route1 - parameter with spaces and special characters" {
            val route = Route1("/title/{name}")

            route.build("Hello World & More!") shouldBe "#/title/Hello%20World%20%26%20More!"
        }

        "Route1 - matching parameter with spaces and special characters" {
            val route = Route1("/title/{name}")

            val match = route.match("/title/Hello World & More!")
            match shouldNotBe null
            match?.routeParams shouldBe mapOf("name" to "Hello World & More!")
        }

        "Route1 - root level parameter" {
            val route = Route1("/{slug}")

            route.build("homepage") shouldBe "#/homepage"

            val match = route.match("/about")
            match shouldNotBe null
            match?.routeParams shouldBe mapOf("slug" to "about")
        }

        "Route1 - deeply nested parameter" {
            val route = Route1("/api/v2/users/profile/{userId}/settings")

            route.build("user123") shouldBe "#/api/v2/users/profile/user123/settings"

            val match = route.match("/api/v2/users/profile/user456/settings")
            match shouldNotBe null
            match?.routeParams shouldBe mapOf("userId" to "user456")
        }

        // URL encoding/decoding round-trip tests
        "Route1 - round-trip with spaces - build then match should return original value" {
            val route = Route1("/search/{query}")
            val originalValue = "hello world"

            val builtUri = route.build(originalValue)
            val match = route.match(builtUri.removePrefix("#"))

            match shouldNotBe null
            match?.routeParams?.get("query") shouldBe originalValue
        }

        "Route1 - round-trip with special characters - build then match should return original value" {
            val route = Route1("/file/{name}")
            val originalValue = "my file & data.txt"

            val builtUri = route.build(originalValue)
            val match = route.match(builtUri.removePrefix("#"))

            match shouldNotBe null
            match?.routeParams?.get("name") shouldBe originalValue
        }

        "Route1 - round-trip with URL characters - build then match should return original value" {
            val route = Route1("/redirect/{url}")
            val originalValue = "https://example.com/path?param=value"

            val builtUri = route.build(originalValue)
            val match = route.match(builtUri.removePrefix("#"))

            match shouldNotBe null
            match?.routeParams?.get("url") shouldBe originalValue
        }

        "Route1 - round-trip with percent signs - build then match should return original value" {
            val route = Route1("/discount/{code}")
            val originalValue = "SAVE25%NOW"

            val builtUri = route.build(originalValue)
            val match = route.match(builtUri.removePrefix("#"))

            match shouldNotBe null
            match?.routeParams?.get("code") shouldBe originalValue
        }

        "Route1 - round-trip with plus signs - build then match should return original value" {
            val route = Route1("/calculation/{formula}")
            val originalValue = "a+b=c"

            val builtUri = route.build(originalValue)
            val match = route.match(builtUri.removePrefix("#"))

            match shouldNotBe null
            match?.routeParams?.get("formula") shouldBe originalValue
        }

        "Route1 - round-trip with hash symbols - build then match should return original value" {
            val route = Route1("/tag/{hashtag}")
            val originalValue = "#awesome"

            val builtUri = route.build(originalValue)
            val match = route.match(builtUri.removePrefix("#"))

            match shouldNotBe null
            match?.routeParams?.get("hashtag") shouldBe originalValue
        }

        "Route1 - round-trip with unicode characters - build then match should return original value" {
            val route = Route1("/user/{name}")
            val originalValue = "José María"

            val builtUri = route.build(originalValue)
            val match = route.match(builtUri.removePrefix("#"))

            match shouldNotBe null
            match?.routeParams?.get("name") shouldBe originalValue
        }

        "Route1 - round-trip with emojis - build then match should return original value" {
            val route = Route1("/reaction/{emoji}")
            val originalValue = "👍🎉"

            val builtUri = route.build(originalValue)
            val match = route.match(builtUri.removePrefix("#"))

            match shouldNotBe null
            match?.routeParams?.get("emoji") shouldBe originalValue
        }

        "Route1 - round-trip with forward slashes - build then match should return original value" {
            val route = Route1("/path/{segment}")
            val originalValue = "folder/subfolder"

            val builtUri = route.build(originalValue)
            val match = route.match(builtUri.removePrefix("#"))

            match shouldNotBe null
            match?.routeParams?.get("segment") shouldBe originalValue
        }

        "Route1 - round-trip with question marks - build then match should return original value" {
            val route = Route1("/query/{question}")
            val originalValue = "What is love?"

            val builtUri = route.build(originalValue)
            val match = route.match(builtUri.removePrefix("#"))

            match shouldNotBe null
            match?.routeParams?.get("question") shouldBe originalValue
        }

        "Route1 - matching URL-encoded parameter should decode correctly" {
            val route = Route1("/search/{term}")

            val match = route.match("/search/hello%20world")
            match shouldNotBe null
            match?.routeParams?.get("term") shouldBe "hello world"
        }

        "Route1 - matching parameter with encoded special characters should decode correctly" {
            val route = Route1("/file/{name}")

            val match = route.match("/file/my%20file%20%26%20data.txt")
            match shouldNotBe null
            match?.routeParams?.get("name") shouldBe "my file & data.txt"
        }

        "Route1 - matching parameter with encoded URL should decode correctly" {
            val route = Route1("/redirect/{url}")

            val match = route.match("/redirect/https%3A%2F%2Fexample.com%2Fpath%3Fparam%3Dvalue")
            match shouldNotBe null
            match?.routeParams?.get("url") shouldBe "https://example.com/path?param=value"
        }
    }
}
