package io.peekandpoke.funktor.demo.server.api.showcase

import io.peekandpoke.funktor.demo.common.showcase.EchoResponse
import io.peekandpoke.funktor.demo.common.showcase.ItemResponse
import io.peekandpoke.funktor.demo.common.showcase.ServerTimeResponse
import io.peekandpoke.funktor.demo.common.showcase.ShowcaseApiClient
import io.peekandpoke.funktor.demo.common.showcase.TransformResponse
import io.peekandpoke.funktor.rest.ApiRoutes
import io.peekandpoke.funktor.rest.docs.codeGen
import io.peekandpoke.funktor.rest.docs.docs
import io.peekandpoke.ultra.remote.ApiResponse
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class RestShowcaseApi : ApiRoutes("showcase-rest") {

    data class EchoParams(val message: String)
    data class ItemParams(val id: String)

    val getPlain = ShowcaseApiClient.GetPlain.mount {
        docs {
            name = "Get server time (plain route demo)"
        }.codeGen {
            funcName = "getPlain"
        }.authorize {
            public()
        }.handle {
            val now = Instant.now()

            ApiResponse.ok(
                ServerTimeResponse(
                    timestamp = DateTimeFormatter.ISO_INSTANT.format(now),
                    epochMs = now.toEpochMilli(),
                )
            )
        }
    }

    val getEcho = ShowcaseApiClient.GetEcho.mount(EchoParams::class) {
        docs {
            name = "Echo message (params route demo)"
        }.codeGen {
            funcName = "getEcho"
        }.authorize {
            public()
        }.handle { params ->
            val now = Instant.now()

            ApiResponse.ok(
                EchoResponse(
                    message = params.message,
                    echoedAt = DateTimeFormatter.ISO_INSTANT.format(now),
                )
            )
        }
    }

    val postTransform = ShowcaseApiClient.PostTransform.mount {
        docs {
            name = "Transform text (body route demo)"
        }.codeGen {
            funcName = "postTransform"
        }.authorize {
            public()
        }.handle { body ->
            val transformed = when (body.operation) {
                "uppercase" -> body.text.uppercase()
                "lowercase" -> body.text.lowercase()
                "reverse" -> body.text.reversed()
                "base64" -> java.util.Base64.getEncoder().encodeToString(body.text.toByteArray())
                else -> body.text.uppercase()
            }

            ApiResponse.ok(
                TransformResponse(
                    original = body.text,
                    transformed = transformed,
                    operation = body.operation,
                )
            )
        }
    }

    val putItem = ShowcaseApiClient.PutItem.mount(ItemParams::class) {
        docs {
            name = "Update item (params + body route demo)"
        }.codeGen {
            funcName = "putItem"
        }.authorize {
            public()
        }.handle { params, body ->
            val now = Instant.now()

            ApiResponse.ok(
                ItemResponse(
                    id = params.id,
                    name = body.name,
                    updatedAt = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(now.atOffset(ZoneOffset.UTC)),
                )
            )
        }
    }
}
