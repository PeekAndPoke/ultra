package de.peekandpoke.ultra.common.remote

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive

class ApiSerializersSpec : StringSpec() {

    init {
        "api() should wrap a serializer with ApiResponse serializer" {
            // Create mock serializer
            val mockSerializer = JsonPrimitive.serializer()

            // Get the wrapped serializer
            val wrappedSerializer = mockSerializer.api()

            // Check descriptor properties
            wrappedSerializer.descriptor.serialName shouldContain "ApiResponse"
            wrappedSerializer.descriptor.getElementDescriptor(1).serialName shouldContain "JsonPrimitive"
        }

        "apiList() should wrap a serializer as List and with ApiResponse serializer" {
            // Create mock serializer
            val mockSerializer = JsonPrimitive.serializer()

            // Get the wrapped serializer
            val wrappedSerializer = mockSerializer.apiList()

            // Check descriptor properties
            wrappedSerializer.descriptor.serialName shouldContain "ApiResponse"
            wrappedSerializer.descriptor.getElementDescriptor(1).serialName shouldContain "List"
        }

        "apiPaged() should wrap a serializer as Paged and with ApiResponse serializer" {
            // Create mock serializer
            val mockSerializer = JsonPrimitive.serializer()

            // Get the wrapped serializer
            val wrappedSerializer = mockSerializer.apiPaged()

            // Check descriptor properties
            wrappedSerializer.descriptor.serialName shouldContain "ApiResponse"
            wrappedSerializer.descriptor.getElementDescriptor(1).serialName shouldContain "Paged"
        }

        "api() serializer should handle deserialization correctly" {
            // Assuming we have a JsonElement serializer
            val stringSerializer = JsonPrimitive.serializer()
            val apiSerializer = stringSerializer.api()

            // Create a test JSON response
            val json = """
                {
                    "status": {
                        "value": 200,
                        "description": "Success"
                    },
                    "data": "Test data"
                }
            """.trimIndent()

            // Parse the JSON
            val response = Json.decodeFromString(apiSerializer, json)

            // Verify the parsed result
            response.status.value shouldBe 200
            response.status.description shouldBe "Success"
            response.data.toString() shouldContain "Test data"
        }

        "apiList() serializer should handle deserialization correctly" {
            // Assuming we have a String serializer
            val stringSerializer = JsonPrimitive.serializer()
            val apiListSerializer = stringSerializer.apiList()

            // Create a test JSON response
            val json = """
                {
                    "status": {
                        "value": 200,
                        "description": "Success"
                    },
                    "data": ["item1", "item2", "item3"]
                }
            """.trimIndent()

            // Parse the JSON
            val response = Json.decodeFromString(apiListSerializer, json)

            // Verify the parsed result
            response.status.value shouldBe 200
            response.status.description shouldBe "Success"
            response.data.shouldNotBeNull()
            response.data.toString() shouldContain "item1"
            response.data.toString() shouldContain "item2"
            response.data.toString() shouldContain "item3"
            response.data.size shouldBe 3
        }

        "apiPaged() serializer should handle deserialization correctly" {
            // Assuming we have a String serializer
            val stringSerializer = JsonPrimitive.serializer()
            val apiPagedSerializer = stringSerializer.apiPaged()

            // Create a test JSON response
            val json = """
                {
                    "status": {
                        "value": 200,
                        "description": "Success"
                    },
                    "data": {
                        "items": ["item1", "item2"],
                        "fullItemCount": 100,
                        "page": 1,
                        "epp": 2
                    }
                }
            """.trimIndent()

            // Parse the JSON
            val response = Json.decodeFromString(apiPagedSerializer, json)

            // Verify the parsed result
            response.status.value shouldBe 200
            response.status.description shouldBe "Success"
            response.data.shouldNotBeNull()
            response.data.items.toString() shouldContain "item1"
            response.data.items.toString() shouldContain "item2"
            response.data.fullItemCount shouldBe 100
            response.data.page shouldBe 1
            response.data.epp shouldBe 2
        }

        "serializers should handle error responses correctly" {
            // Create a serializer for handling errors
            val stringSerializer = JsonPrimitive.serializer()
            val apiSerializer = stringSerializer.api()

            // Create a test error JSON response
            val json = """
                {
                    "status": {
                        "value": 404,
                        "description": "Not found"
                    },
                    "data": null
                }
            """.trimIndent()

            // Parse the JSON
            val response = Json.decodeFromString(apiSerializer, json)

            // Verify the parsed result
            response.status.value shouldBe 404
            response.status.description shouldBe "Not found"
            response.data shouldBe null
            response.isSuccess() shouldBe false
        }

        "serializers should handle composite types correctly" {
            // Define a custom serializer for a more complex type
            val userSerializer = kotlinx.serialization.json.JsonObject.serializer()

            // Create the wrapped serializers
            val apiUserSerializer = userSerializer.api()
            val apiUserListSerializer = userSerializer.apiList()
            val apiUserPagedSerializer = userSerializer.apiPaged()

            // Verify descriptor names
            apiUserSerializer.descriptor.serialName shouldContain "ApiResponse"
            apiUserListSerializer.descriptor.serialName shouldContain "ApiResponse"
            apiUserPagedSerializer.descriptor.serialName shouldContain "ApiResponse"
        }
    }
}
