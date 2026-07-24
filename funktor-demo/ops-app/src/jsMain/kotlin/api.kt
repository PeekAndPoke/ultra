package io.peekandpoke.funktor.demo.opsapp

import io.ktor.client.HttpClient
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.plugins.sse.SSE
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.peekandpoke.funktor.auth.api.AuthApiClient
import io.peekandpoke.funktor.auth.model.RealmId
import io.peekandpoke.funktor.demo.common.operator.OperatorApiClient
import io.peekandpoke.funktor.saas.api.OrgsApiClient
import io.peekandpoke.ultra.remote.ApiClient.Config
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

    val auth = AuthApiClient(realm = RealmId("operators"), config = config)

    val orgs = OrgsApiClient(config = config)

    val operator = OperatorApiClient(config = config)
}
