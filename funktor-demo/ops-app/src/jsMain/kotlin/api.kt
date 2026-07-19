package io.peekandpoke.funktor.demo.opsapp

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.sse.SSE
import io.ktor.serialization.kotlinx.json.json
import io.peekandpoke.funktor.auth.api.AuthApiClient
import io.peekandpoke.funktor.saas.api.OrgsApiClient
import io.peekandpoke.ultra.remote.ApiClient.Config
import io.peekandpoke.ultra.remote.ErrorLoggingResponseInterceptor
import io.peekandpoke.ultra.remote.SetBearerRequestInterceptor
import kotlinx.serialization.json.Json

class OpsAppApis(appConfig: OpsAppConfig, tokenProvider: () -> String?) {

    val codec = Json {
        classDiscriminator = "_type"
        ignoreUnknownKeys = true
        isLenient = true
        prettyPrint = true
    }

    val config = Config(
        baseUrl = appConfig.apiBaseUrl,
        codec = codec,
        requestInterceptors = listOf(
            SetBearerRequestInterceptor(tokenProvider)
        ),
        responseInterceptors = listOf(
            ErrorLoggingResponseInterceptor()
        ),
        client = HttpClient {
            install(SSE) {
                showCommentEvents()
                showRetryEvents()
            }

            install(ContentNegotiation) {
                json(json = codec)
            }
        },
    )

    val auth = AuthApiClient(realm = "operators", config = config)

    val orgs = OrgsApiClient(config = config)
}
