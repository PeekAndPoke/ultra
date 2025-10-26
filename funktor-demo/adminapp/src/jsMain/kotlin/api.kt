package de.peekandpoke.funktor.demo.adminapp

import de.peekandpoke.funktor.auth.api.AuthApiClient
import de.peekandpoke.funktor.cluster.devtools.DevtoolsApiResponseInterceptor
import de.peekandpoke.ultra.common.remote.ApiClient.Config
import de.peekandpoke.ultra.common.remote.ErrorLoggingResponseInterceptor
import de.peekandpoke.ultra.common.remote.SetBearerRequestInterceptor
import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.sse.*
import io.ktor.serialization.kotlinx.json.*
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
}


