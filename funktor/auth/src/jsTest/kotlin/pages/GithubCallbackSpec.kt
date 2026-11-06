package io.peekandpoke.funktor.auth.pages

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.peekandpoke.funktor.auth.AuthSessionConfig
import io.peekandpoke.funktor.auth.AuthState
import io.peekandpoke.funktor.auth.api.AuthApiClient
import io.peekandpoke.funktor.auth.model.AuthProviderModel
import io.peekandpoke.funktor.auth.model.AuthRealmModel
import io.peekandpoke.funktor.auth.model.PasswordPolicy
import io.peekandpoke.funktor.auth.model.RealmId
import io.peekandpoke.funktor.auth.widgets.GithubOAuth
import io.peekandpoke.kraft.routing.Static
import io.peekandpoke.kraft.testing.TestBed
import io.peekandpoke.ultra.remote.ApiClient
import io.peekandpoke.ultra.remote.ApiResponse
import io.peekandpoke.ultra.remote.api
import kotlinx.browser.window
import kotlinx.coroutines.delay
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/**
 * The login page acting on GitHub's callback (ULTRA-00288): only a callback that brings back the state THIS browser
 * stored signs anyone in, and it signs in or up as the button chose — not as a query parameter says.
 */
class GithubCallbackSpec : StringSpec({

    val codec = Json { ignoreUnknownKeys = true }

    val github = AuthProviderModel(
        id = "github",
        type = AuthProviderModel.TYPE_GITHUB,
        capabilities = setOf(AuthProviderModel.Capability.SignIn, AuthProviderModel.Capability.SignUp),
        config = buildJsonObject { put("client-id", "cid") },
    )
    val realm = AuthRealmModel(id = RealmId("test"), providers = listOf(github), passwordPolicy = PasswordPolicy.default)

    /** Serves the realm; records every POST and its body, and lets those fail in transport. */
    class FakeAuthServer {
        val posts = mutableListOf<Pair<String, JsonObject?>>()

        val client = HttpClient(
            MockEngine { request ->
                val path = request.url.encodedPath

                if (request.method.value == "POST") {
                    posts += path to (request.body as? TextContent)?.let { codec.parseToJsonElement(it.text).jsonObject }
                    error("not answered")
                }

                respond(
                    content = codec.encodeToString(AuthRealmModel.serializer().api(), ApiResponse.ok(realm)),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }
        )
    }

    fun stateOn(server: FakeAuthServer) = AuthState(
        userSerializer = JsonObject.serializer(),
        frontend = AuthFrontend.default(config = AuthFrontendConfig(redirectAfterLogin = Static("/")())),
        api = AuthApiClient(
            realm = RealmId("test"),
            config = ApiClient.Config(baseUrl = "http://auth.test", codec = codec, client = server.client),
        ),
        router = { error("this spec does not navigate") },
        sessionConfig = AuthSessionConfig(enabled = false),
    )

    val page = window.location.pathname

    /** What a callback did: the POSTs, and the URL's query and hash afterwards. */
    data class Outcome(val posts: List<Pair<String, JsonObject?>>, val search: String, val hash: String)

    /** Opens the login page as GitHub's redirect does, with [state] (and GitHub's [answer]); what it did. */
    suspend fun callbackWith(state: String?, answer: String = "code=the-code"): Outcome {
        val query = "?${LoginController.AUTH_CALLBACK_PARAM}=github&$answer" + (state?.let { "&state=$it" } ?: "")
        window.history.replaceState(null, "", page + query)

        val server = FakeAuthServer()

        try {
            TestBed.preact({ AuthLogin(state = stateOn(server)) }) {
                delay(400)
            }

            return Outcome(server.posts, window.location.search, window.location.hash)
        } finally {
            window.history.replaceState(null, "", page)
        }
    }

    beforeTest { window.sessionStorage.removeItem(GithubOAuth.STORAGE_KEY) }

    "a callback with a FORGED state, or none, signs nobody in" {
        listOf("forged", null).forEach { forged ->
            withClue("state $forged") {
                GithubOAuth.begin("cid", "https://app.test/login", GithubOAuth.Action.SignIn, hash = "")

                callbackWith(forged).posts shouldBe emptyList()
            }
        }
    }

    "a callback no button in this browser started signs nobody in" {
        callbackWith("anything").posts shouldBe emptyList()
    }

    "the stored state signs in or up, as the button chose, with GitHub's code" {
        listOf(GithubOAuth.Action.SignIn to "/auth/test/signin", GithubOAuth.Action.SignUp to "/auth/test/signup")
            .forEach { (action, path) ->
                withClue(action) {
                    val url = GithubOAuth.begin("cid", "https://app.test/login", action, hash = "")
                    val state = url.substringAfter("state=")

                    val posts = callbackWith(state).posts

                    posts.map { it.first } shouldBe listOf(path)
                    posts.single().second?.get("token")?.jsonPrimitive?.content shouldBe "the-code"
                }
            }
    }

    "after the callback, the URL is clean and back on the route the round trip started on, also when GitHub refused" {
        listOf("code=the-code", "error=access_denied").forEach { answer ->
            withClue(answer) {
                val state = GithubOAuth.begin("cid", "https://app.test/login", GithubOAuth.Action.SignIn, hash = "#/login")
                    .substringAfter("state=")

                val outcome = callbackWith(state, answer = answer)

                outcome.search shouldBe ""
                outcome.hash shouldBe "#/login"
            }
        }
    }

    "a hash-routed app's ROOT gets the callback: creating the auth state puts back the route it started on" {
        val state = GithubOAuth.begin("cid", "https://app.test/", GithubOAuth.Action.SignUp, hash = "#/auth/login")
            .substringAfter("state=")

        try {
            withClue("no callback in the query: nothing moves") {
                window.history.replaceState(null, "", page)
                stateOn(FakeAuthServer())
                window.location.hash shouldBe ""
            }

            withClue("the callback of a round trip this tab started: back on its route, the query kept for the login") {
                window.history.replaceState(null, "", "$page?${LoginController.AUTH_CALLBACK_PARAM}=github&code=c&state=$state")
                stateOn(FakeAuthServer())
                window.location.hash shouldBe "#/auth/login"
                window.location.search shouldContain "state=$state"
            }
        } finally {
            window.history.replaceState(null, "", page)
        }
    }

    "a callback of a round trip this tab did NOT start moves nothing" {
        try {
            window.history.replaceState(null, "", "$page?${LoginController.AUTH_CALLBACK_PARAM}=github&code=c&state=s")
            stateOn(FakeAuthServer())
            window.location.hash shouldBe ""
        } finally {
            window.history.replaceState(null, "", page)
        }
    }
})
