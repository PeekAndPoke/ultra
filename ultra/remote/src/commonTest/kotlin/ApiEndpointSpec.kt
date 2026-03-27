package io.peekandpoke.ultra.remote

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf

class ApiEndpointSpec : StringSpec({

    "Get should store the uri" {
        val endpoint = ApiEndpoint.Get("/api/users")
        endpoint.uri shouldBe "/api/users"
    }

    "Post should store the uri" {
        val endpoint = ApiEndpoint.Post("/api/users")
        endpoint.uri shouldBe "/api/users"
    }

    "Put should store the uri" {
        val endpoint = ApiEndpoint.Put("/api/users/{id}")
        endpoint.uri shouldBe "/api/users/{id}"
    }

    "Delete should store the uri" {
        val endpoint = ApiEndpoint.Delete("/api/users/{id}")
        endpoint.uri shouldBe "/api/users/{id}"
    }

    "All subtypes should be instances of ApiEndpoint" {
        val get: ApiEndpoint = ApiEndpoint.Get("/get")
        val post: ApiEndpoint = ApiEndpoint.Post("/post")
        val put: ApiEndpoint = ApiEndpoint.Put("/put")
        val delete: ApiEndpoint = ApiEndpoint.Delete("/delete")

        get.shouldBeInstanceOf<ApiEndpoint.Get>()
        post.shouldBeInstanceOf<ApiEndpoint.Post>()
        put.shouldBeInstanceOf<ApiEndpoint.Put>()
        delete.shouldBeInstanceOf<ApiEndpoint.Delete>()
    }

    "Data class equality should work correctly" {
        val endpoint1 = ApiEndpoint.Get("/api/users")
        val endpoint2 = ApiEndpoint.Get("/api/users")
        val endpoint3 = ApiEndpoint.Get("/api/posts")

        endpoint1 shouldBe endpoint2
        (endpoint1 == endpoint3) shouldBe false
    }

    "Data class copy should work correctly" {
        val original = ApiEndpoint.Get("/api/users")
        val copied = original.copy(uri = "/api/posts")

        original.uri shouldBe "/api/users"
        copied.uri shouldBe "/api/posts"
    }

    "Different method types with the same uri should not be equal" {
        val get = ApiEndpoint.Get("/api/users")
        val post = ApiEndpoint.Post("/api/users")

        (get == post) shouldBe false
    }
})
