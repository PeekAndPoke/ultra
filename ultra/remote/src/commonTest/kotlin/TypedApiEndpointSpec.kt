package io.peekandpoke.ultra.remote

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.serialization.builtins.serializer

class TypedApiEndpointSpec : StringSpec({

    // Delete //////////////////////////////////////////////////////////////////////////////////////

    "Delete should store uri and response serializer" {
        val endpoint = TypedApiEndpoint.Delete(
            uri = "/api/items/{id}",
            response = String.serializer(),
        )

        endpoint.uri shouldBe "/api/items/{id}"
    }

    "Delete.bind should create a Bound with the given params" {
        val endpoint = TypedApiEndpoint.Delete(
            uri = "/api/items/{id}",
            response = String.serializer(),
        )

        val bound = endpoint.bind(mapOf("id" to "123"))

        bound.endpoint shouldBe endpoint
        bound.params shouldBe mapOf("id" to "123")
    }

    "Delete invoke with vararg params" {
        val endpoint = TypedApiEndpoint.Delete(
            uri = "/api/items/{id}",
            response = String.serializer(),
        )

        val bound = endpoint("id" to "456")

        bound.params shouldBe mapOf("id" to "456")
    }

    "Delete invoke with map params" {
        val endpoint = TypedApiEndpoint.Delete(
            uri = "/api/items/{id}",
            response = String.serializer(),
        )

        val bound = endpoint(mapOf("id" to "789"))

        bound.params shouldBe mapOf("id" to "789")
    }

    "Delete companion invoke should default to EmptyObject serializer" {
        val endpoint = TypedApiEndpoint.Delete(uri = "/api/items/{id}")

        endpoint.uri shouldBe "/api/items/{id}"
    }

    // Get /////////////////////////////////////////////////////////////////////////////////////////

    "Get should store uri and response serializer" {
        val endpoint = TypedApiEndpoint.Get(
            uri = "/api/items",
            response = String.serializer(),
        )

        endpoint.uri shouldBe "/api/items"
    }

    "Get invoke with map params" {
        val endpoint = TypedApiEndpoint.Get(
            uri = "/api/items/{id}",
            response = String.serializer(),
        )

        val bound = endpoint(params = mapOf("id" to "42"))

        bound.uri shouldBe "/api/items/{id}"
        bound.params shouldBe mapOf("id" to "42")
        bound.responseSerializer shouldBe endpoint.response
    }

    "Get invoke with vararg params" {
        val endpoint = TypedApiEndpoint.Get(
            uri = "/api/items/{id}",
            response = String.serializer(),
        )

        val bound = endpoint("id" to "42")

        bound.params shouldBe mapOf("id" to "42")
    }

    "Get invoke with empty params" {
        val endpoint = TypedApiEndpoint.Get(
            uri = "/api/items",
            response = String.serializer(),
        )

        val bound = endpoint()

        bound.params shouldBe emptyMap()
    }

    // Head ////////////////////////////////////////////////////////////////////////////////////////

    "Head should store uri and response serializer" {
        val endpoint = TypedApiEndpoint.Head(
            uri = "/api/items/{id}",
            response = String.serializer(),
        )

        endpoint.uri shouldBe "/api/items/{id}"
    }

    "Head companion invoke should default to EmptyObject serializer" {
        val endpoint = TypedApiEndpoint.Head(uri = "/api/check")

        endpoint.uri shouldBe "/api/check"
    }

    "Head.bind should create a Bound with the given params" {
        val endpoint = TypedApiEndpoint.Head(
            uri = "/api/items/{id}",
            response = String.serializer(),
        )

        val bound = endpoint.bind(mapOf("id" to "1"))

        bound.endpoint shouldBe endpoint
        bound.params shouldBe mapOf("id" to "1")
    }

    // Sse /////////////////////////////////////////////////////////////////////////////////////////

    "Sse should store uri" {
        val endpoint = TypedApiEndpoint.Sse(uri = "/api/events")

        endpoint.uri shouldBe "/api/events"
    }

    "Sse invoke with map params" {
        val endpoint = TypedApiEndpoint.Sse(uri = "/api/events/{channel}")

        val bound = endpoint(mapOf("channel" to "updates"))

        bound.uri shouldBe "/api/events/{channel}"
        bound.params shouldBe mapOf("channel" to "updates")
    }

    "Sse invoke with vararg params" {
        val endpoint = TypedApiEndpoint.Sse(uri = "/api/events/{channel}")

        val bound = endpoint("channel" to "updates")

        bound.params shouldBe mapOf("channel" to "updates")
    }

    // Post ////////////////////////////////////////////////////////////////////////////////////////

    "Post should store uri and serializers" {
        val endpoint = TypedApiEndpoint.Post(
            uri = "/api/items",
            body = String.serializer(),
            response = Int.serializer(),
        )

        endpoint.uri shouldBe "/api/items"
    }

    "Post invoke with body and map params" {
        val endpoint = TypedApiEndpoint.Post(
            uri = "/api/items/{category}",
            body = String.serializer(),
            response = Int.serializer(),
        )

        val bound = endpoint(
            params = mapOf("category" to "books"),
            body = "new item",
        )

        bound.uri shouldBe "/api/items/{category}"
        bound.params shouldBe mapOf("category" to "books")
        bound.body shouldBe "new item"
    }

    "Post invoke with body and vararg params" {
        val endpoint = TypedApiEndpoint.Post(
            uri = "/api/items/{category}",
            body = String.serializer(),
            response = Int.serializer(),
        )

        val bound = endpoint(
            "category" to "books",
            body = "new item",
        )

        bound.params shouldBe mapOf("category" to "books")
        bound.body shouldBe "new item"
    }

    // Put /////////////////////////////////////////////////////////////////////////////////////////

    "Put should store uri and serializers" {
        val endpoint = TypedApiEndpoint.Put(
            uri = "/api/items/{id}",
            body = String.serializer(),
            response = Int.serializer(),
        )

        endpoint.uri shouldBe "/api/items/{id}"
    }

    "Put invoke with body and map params" {
        val endpoint = TypedApiEndpoint.Put(
            uri = "/api/items/{id}",
            body = String.serializer(),
            response = Int.serializer(),
        )

        val bound = endpoint(
            params = mapOf("id" to "42"),
            body = "updated",
        )

        bound.uri shouldBe "/api/items/{id}"
        bound.params shouldBe mapOf("id" to "42")
        bound.body shouldBe "updated"
    }

    "Put invoke with body and vararg params" {
        val endpoint = TypedApiEndpoint.Put(
            uri = "/api/items/{id}",
            body = String.serializer(),
            response = Int.serializer(),
        )

        val bound = endpoint(
            "id" to "42",
            body = "updated",
        )

        bound.params shouldBe mapOf("id" to "42")
        bound.body shouldBe "updated"
    }

    // All subtypes are TypedApiEndpoint ///////////////////////////////////////////////////////////

    "All subtypes should be instances of TypedApiEndpoint" {
        val delete: TypedApiEndpoint = TypedApiEndpoint.Delete(uri = "/d", response = String.serializer())
        val get: TypedApiEndpoint = TypedApiEndpoint.Get(uri = "/g", response = String.serializer())
        val head: TypedApiEndpoint = TypedApiEndpoint.Head(uri = "/h", response = String.serializer())
        val sse: TypedApiEndpoint = TypedApiEndpoint.Sse(uri = "/s")
        val post: TypedApiEndpoint = TypedApiEndpoint.Post(
            uri = "/p", body = String.serializer(), response = String.serializer()
        )
        val put: TypedApiEndpoint = TypedApiEndpoint.Put(
            uri = "/u", body = String.serializer(), response = String.serializer()
        )

        delete.shouldBeInstanceOf<TypedApiEndpoint.Delete<*>>()
        get.shouldBeInstanceOf<TypedApiEndpoint.Get<*>>()
        head.shouldBeInstanceOf<TypedApiEndpoint.Head<*>>()
        sse.shouldBeInstanceOf<TypedApiEndpoint.Sse>()
        post.shouldBeInstanceOf<TypedApiEndpoint.Post<*, *>>()
        put.shouldBeInstanceOf<TypedApiEndpoint.Put<*, *>>()
    }

    // withAttributes //////////////////////////////////////////////////////////////////////////////

    "Get.withAttributes should return a copy with merged attributes" {
        val endpoint = TypedApiEndpoint.Get(
            uri = "/api/items",
            response = String.serializer(),
        )

        val updated = endpoint.withAttributes { }

        updated.uri shouldBe endpoint.uri
    }

    "Delete.withAttributes should return a copy with merged attributes" {
        val endpoint = TypedApiEndpoint.Delete(
            uri = "/api/items/{id}",
            response = String.serializer(),
        )

        val updated = endpoint.withAttributes { }

        updated.uri shouldBe endpoint.uri
    }

    "Post.withAttributes should return a copy with merged attributes" {
        val endpoint = TypedApiEndpoint.Post(
            uri = "/api/items",
            body = String.serializer(),
            response = String.serializer(),
        )

        val updated = endpoint.withAttributes { }

        updated.uri shouldBe endpoint.uri
    }

    "Put.withAttributes should return a copy with merged attributes" {
        val endpoint = TypedApiEndpoint.Put(
            uri = "/api/items/{id}",
            body = String.serializer(),
            response = String.serializer(),
        )

        val updated = endpoint.withAttributes { }

        updated.uri shouldBe endpoint.uri
    }

    "Sse.withAttributes should return a copy with merged attributes" {
        val endpoint = TypedApiEndpoint.Sse(uri = "/api/events")

        val updated = endpoint.withAttributes { }

        updated.uri shouldBe endpoint.uri
    }

    "Head.withAttributes should return a copy with merged attributes" {
        val endpoint = TypedApiEndpoint.Head(
            uri = "/api/check",
            response = String.serializer(),
        )

        val updated = endpoint.withAttributes { }

        updated.uri shouldBe endpoint.uri
    }
})
