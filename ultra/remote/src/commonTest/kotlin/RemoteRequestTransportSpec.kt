package io.peekandpoke.ultra.remote

import io.kotest.assertions.throwables.shouldThrowAny
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

/**
 * Behaviour of the unified Ktor-backed transport ([RemoteRequestImpl]). Locks the emit-on-non-2xx
 * contract (and its `expectSuccess=false` enforcement), transport-failure propagation, and
 * observer isolation — the core of the ktor-client unification, previously covered only by a
 * (partly inaccurate) manual code trace.
 */
class RemoteRequestTransportSpec : StringSpec({

    val codec = Json { ignoreUnknownKeys = true }

    fun config(
        client: HttpClient,
        observers: List<suspend (RemoteResponse) -> Unit> = emptyList(),
    ) = ApiClient.Config(baseUrl = "https://api.test", codec = codec, client = client, onResponse = observers)

    fun respondingClient(
        status: HttpStatusCode,
        body: String,
        expectSuccess: Boolean = false,
    ) = HttpClient(
        MockEngine {
            respond(content = body, status = status, headers = headersOf(HttpHeaders.ContentType, "application/json"))
        }
    ) {
        this.expectSuccess = expectSuccess
    }

    "a non-2xx response is emitted (not thrown), carrying status and body" {
        val remote = createRequest(config(respondingClient(HttpStatusCode.BadRequest, "the-error-body")))

        val resp = remote.get("/x").first()

        resp.status shouldBe 400
        resp.ok shouldBe false
        resp.is4xx shouldBe true
        resp.body shouldBe "the-error-body"
    }

    "a client-level expectSuccess=true is overridden — a 4xx still emits instead of throwing" {
        val remote = createRequest(
            config(respondingClient(HttpStatusCode.Forbidden, "nope", expectSuccess = true))
        )

        val resp = remote.get("/x").first()

        resp.status shouldBe 403
        resp.ok shouldBe false
    }

    "a genuine transport failure propagates through the flow" {
        val remote = createRequest(config(HttpClient(MockEngine { throw RuntimeException("connection reset") })))

        shouldThrowAny { remote.get("/x").first() }
    }

    "onResponse observers run once per response, including error responses" {
        var count = 0
        val remote = createRequest(
            config(respondingClient(HttpStatusCode.InternalServerError, "boom"), observers = listOf { count++ })
        )

        remote.get("/x").first()

        count shouldBe 1
    }

    "a throwing observer does not break response delivery" {
        val remote = createRequest(
            config(respondingClient(HttpStatusCode.OK, "ok-body"), observers = listOf { error("observer boom") })
        )

        val resp = remote.get("/x").first()

        resp.ok shouldBe true
        resp.body shouldBe "ok-body"
    }
})
