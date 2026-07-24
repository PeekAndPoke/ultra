package io.peekandpoke.funktor.demo.b2b2capp

import io.ktor.client.HttpClient
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.peekandpoke.funktor.auth.api.AuthApiClient
import io.peekandpoke.funktor.auth.model.RealmId
import io.peekandpoke.ultra.remote.ApiClient.Config
import kotlinx.serialization.json.Json

class B2b2cAppApis(appConfig: B2b2cAppConfig, tokenProvider: () -> String?) {

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

    val auth = AuthApiClient(realm = RealmId("b2b2c"), config = config)
}
