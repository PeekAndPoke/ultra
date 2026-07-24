package io.peekandpoke.funktor.demo.b2bapp

import io.ktor.client.HttpClient
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.peekandpoke.funktor.auth.api.AuthApiClient
import io.peekandpoke.funktor.demo.common.b2b.B2bMembersApiClient
import io.peekandpoke.ultra.remote.ApiClient.Config
import kotlinx.serialization.json.Json

class B2bAppApis(appConfig: B2bAppConfig, tokenProvider: () -> String?) {

    val codec = Json {
        classDiscriminator = "_type"
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = true
    }

    val config = Config(
        baseUrl = appConfig.apiBaseUrl,
        codec = codec,
        client = HttpClient {
            install(SSE) {
                showCommentEvents()
                showRetryEvents()
            }

            // Attach the bearer token (evaluated per request) via Ktor's machinery.
            defaultRequest {
                tokenProvider()?.let { header(HttpHeaders.Authorization, "Bearer $it") }
            }
        },
    )

    val auth = AuthApiClient(realm = "b2b", config = config)

    val members = B2bMembersApiClient(config = config)
}
