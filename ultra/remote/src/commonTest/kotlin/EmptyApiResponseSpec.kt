package io.peekandpoke.ultra.remote

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.serialization.json.Json

class EmptyApiResponseSpec : StringSpec({

    "Should deserialize a minimal JSON response" {
        val json = """
            {
                "status": {
                    "value": 200,
                    "description": "OK"
                }
            }
        """.trimIndent()

        val response = Json.decodeFromString(EmptyApiResponse.serializer(), json)

        response.status shouldBe HttpStatusCode.OK
        response.messages shouldBe null
        response.insights shouldBe null
    }

    "Should deserialize a JSON response with messages" {
        val json = """
            {
                "status": {
                    "value": 400,
                    "description": "Bad Request"
                },
                "messages": [
                    {"type": "error", "text": "Validation failed"}
                ]
            }
        """.trimIndent()

        val response = Json.decodeFromString(EmptyApiResponse.serializer(), json)

        response.status shouldBe HttpStatusCode.BadRequest
        response.messages?.size shouldBe 1
        response.messages?.first()?.text shouldBe "Validation failed"
    }

    "Should serialize and deserialize round-trip" {
        val original = EmptyApiResponse(
            status = HttpStatusCode.OK,
            messages = null,
            insights = null,
        )

        val jsonStr = Json.encodeToString(EmptyApiResponse.serializer(), original)
        val deserialized = Json.decodeFromString(EmptyApiResponse.serializer(), jsonStr)

        deserialized shouldBe original
    }

    "Should hold Insights when present" {
        val insights = ApiResponse.Insights(
            ts = 1000L,
            method = "GET",
            url = "/test",
            server = "s1",
            status = HttpStatusCode.OK,
            durationMs = 5.0,
            detailsUri = null,
            detailsUrl = null,
        )

        val response = EmptyApiResponse(
            status = HttpStatusCode.OK,
            messages = null,
            insights = insights,
        )

        response.insights shouldBe insights
        response.insights?.ts shouldBe 1000L
        response.insights?.method shouldBe "GET"
    }
})
