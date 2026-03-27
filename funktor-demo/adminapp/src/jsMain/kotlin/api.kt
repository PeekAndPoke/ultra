package io.peekandpoke.funktor.demo.adminapp

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.sse.*
import io.ktor.serialization.kotlinx.json.*
import io.peekandpoke.funktor.auth.api.AuthApiClient
import io.peekandpoke.funktor.cluster.devtools.DevtoolsApiResponseInterceptor
import io.peekandpoke.funktor.demo.common.showcase.ShowcaseApiClient
import io.peekandpoke.ultra.remote.ApiClient.Config
import io.peekandpoke.ultra.remote.ErrorLoggingResponseInterceptor
import io.peekandpoke.ultra.remote.SetBearerRequestInterceptor
import kotlinx.serialization.json.Json

class AdminAppApis(appConfig: AdminAppConfig, tokenProvider: () -> String?) {

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
            DevtoolsApiResponseInterceptor(),
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

    val auth = AuthApiClient(realm = "admin-user", config = config)

    val showcase = ShowcaseApiClient(config = config)
}
