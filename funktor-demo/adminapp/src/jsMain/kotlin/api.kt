package io.peekandpoke.funktor.demo.adminapp

import io.ktor.client.HttpClient
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.peekandpoke.funktor.auth.api.AuthApiClient
import io.peekandpoke.funktor.auth.model.RealmId
import io.peekandpoke.funktor.demo.common.funktorconf.FunktorConfApiClient
import io.peekandpoke.funktor.demo.common.showcase.ShowcaseApiClient
import io.peekandpoke.funktor.inspect.cluster.devtools.DevtoolsResponseObserver
import io.peekandpoke.ultra.remote.ApiClient.Config
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
        onResponse = listOf(
            { response -> DevtoolsResponseObserver.record(response) },
        ),
    )

    val auth = AuthApiClient(realm = RealmId("admin-user"), config = config)

    val showcase = ShowcaseApiClient(config = config)

    val funktorConf = FunktorConfApiClient(config = config)
}
