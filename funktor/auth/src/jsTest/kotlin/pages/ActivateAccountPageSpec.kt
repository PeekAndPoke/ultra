package io.peekandpoke.funktor.auth.pages

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.peekandpoke.funktor.auth.AuthSessionConfig
import io.peekandpoke.funktor.auth.AuthState
import io.peekandpoke.funktor.auth.api.AuthApiClient
import io.peekandpoke.funktor.auth.model.AuthActivateAccountResponse
import io.peekandpoke.funktor.auth.model.AuthCancelSignUpResponse
import io.peekandpoke.funktor.auth.model.AuthRealmModel
import io.peekandpoke.funktor.auth.model.AuthResendActivationResponse
import io.peekandpoke.funktor.auth.model.AuthSignInRequest
import io.peekandpoke.funktor.auth.model.AuthSignInResponse
import io.peekandpoke.funktor.auth.model.PasswordPolicy
import io.peekandpoke.funktor.auth.model.RealmId
import io.peekandpoke.kraft.routing.Static
import io.peekandpoke.kraft.testing.KQuery
import io.peekandpoke.kraft.testing.TestBed
import io.peekandpoke.kraft.testing.click
import io.peekandpoke.kraft.testing.containsText
import io.peekandpoke.kraft.testing.selectCss
import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.remote.ApiClient
import io.peekandpoke.ultra.remote.ApiResponse
import io.peekandpoke.ultra.remote.api
import kotlinx.coroutines.delay
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.w3c.dom.Element
import kotlin.time.Duration.Companion.minutes

/**
 * The activation page in a real browser, against a fake auth server (a ktor MockEngine — the server is the
 * one thing a browser test cannot run).
 *
 * The property that matters most: **opening a link from the mail sends NOTHING.** Mail gateways open links
 * in a sandbox, so a page that activated on load would activate accounts nobody chose to.
 */
class ActivateAccountPageSpec : StringSpec({

    val codec = Json { ignoreUnknownKeys = true }

    /** Answers one auth endpoint; `null` makes the request fail in transport. */
    class FakeAuthServer {
        val calls = mutableListOf<String>()
        val answers = mutableMapOf<String, () -> String?>()

        fun <T> answer(path: String, serializer: KSerializer<T>, value: () -> T?) {
            answers[path] = { value()?.let { codec.encodeToString(serializer.api(), ApiResponse.ok(it)) } }
        }

        val client = HttpClient(
            MockEngine { request ->
                val path = request.url.encodedPath
                calls += "${request.method.value} $path"

                val body = answers[path]?.invoke() ?: error("the server cannot be reached")

                respond(content = body, status = HttpStatusCode.OK, headers = headersOf(HttpHeaders.ContentType, "application/json"))
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

    suspend fun KQuery<Element>.button(text: String): KQuery<Element> =
        KQuery(selectCss(".button").filter { it.textContent?.contains(text) == true })

    suspend fun KQuery<Element>.awaitText(text: String) {
        repeat(40) {
            if (containsText(text)) return
            delay(50)
        }
        withClue("expected the page to show '$text', it shows: ${first().textContent}") { containsText(text) shouldBe true }
    }

    val activatePath = "/auth/test/activate"
    val cancelPath = "/auth/test/activate/cancel"
    val resendPath = "/auth/test/activate/resend"
    val signInPath = "/auth/test/signin"

    "opening the activation link sends NOTHING — only the Activate click POSTs" {
        val server = FakeAuthServer().apply {
            answer(activatePath, AuthActivateAccountResponse.serializer()) { AuthActivateAccountResponse(success = true) }
        }
        val state = stateOn(server)

        TestBed.preact({ ActivateAccountPage(state = state, provider = "email-password", token = "tok") }) { root ->
            // Give a load-time request every chance to happen.
            delay(300)

            withClue("a scanner that only OPENS the link must change nothing") {
                server.calls shouldBe emptyList()
            }

            root.button("Activate my account").size shouldBe 1
            root.button("cancel this account").size shouldBe 1

            root.button("Activate my account").click()
            root.awaitText("Your account is activated")

            server.calls shouldBe listOf("POST $activatePath")
        }
    }

    "the mail's cancel link leads with Cancel, and its click POSTs the cancel" {
        val server = FakeAuthServer().apply {
            answer(cancelPath, AuthCancelSignUpResponse.serializer()) { AuthCancelSignUpResponse(success = true) }
        }
        val state = stateOn(server)

        TestBed.preact({
            ActivateAccountPage(state = state, provider = "email-password", token = "tok", mode = ActivateAccountPage.Mode.Cancel)
        }) { root ->
            delay(300)
            server.calls shouldBe emptyList()

            root.button("Cancel this sign-up").click()
            root.awaitText("The sign-up is cancelled")

            server.calls shouldBe listOf("POST $cancelPath")
        }
    }

    "a link the server refuses says so" {
        val server = FakeAuthServer().apply {
            answer(activatePath, AuthActivateAccountResponse.serializer()) { AuthActivateAccountResponse(success = false) }
        }
        val state = stateOn(server)

        TestBed.preact({ ActivateAccountPage(state = state, provider = "email-password", token = "tok") }) { root ->
            root.button("Activate my account").click()
            root.awaitText("This link is not valid any more")
        }
    }

    "a server that cannot be reached is NOT reported as a dead link, and Retry repeats the click" {
        // The dead Retry branch of round 2: a transient failure on a valid link used to read "not valid".
        var reachable = false
        val server = FakeAuthServer().apply {
            answer(activatePath, AuthActivateAccountResponse.serializer()) {
                AuthActivateAccountResponse(success = true).takeIf { reachable }
            }
        }
        val state = stateOn(server)

        TestBed.preact({ ActivateAccountPage(state = state, provider = "email-password", token = "tok") }) { root ->
            root.button("Activate my account").click()
            root.awaitText("We could not reach the server")
            root.containsText("not valid any more") shouldBe false

            reachable = true
            root.button("Retry").click()
            root.awaitText("Your account is activated")

            server.calls shouldBe listOf("POST $activatePath", "POST $activatePath")
        }
    }

    "a refused sign-in carries the address and the resend token to the page — which reads it AND clears it" {
        val server = FakeAuthServer().apply {
            answer(signInPath, AuthSignInResponse.serializer()) {
                AuthSignInResponse.ActivationRequired(
                    realm = AuthRealmModel(id = RealmId("test"), providers = emptyList(), passwordPolicy = PasswordPolicy.default),
                    resendToken = "resend-token",
                )
            }
            answer(resendPath, AuthResendActivationResponse.serializer()) { AuthResendActivationResponse(sent = true) }
        }
        val state = stateOn(server)

        state.login(AuthSignInRequest.EmailAndPassword(provider = "email-password", email = "me@test.com", password = "pw"))

        state.pendingActivation.shouldNotBeNull().let {
            it.email shouldBe "me@test.com"
            it.resendToken shouldBe "resend-token"
        }

        TestBed.preact({ ActivateAccountPage(state = state, provider = "email-password", token = null) }) { root ->
            root.awaitText("Your account is not activated yet. We sent a link to me@test.com")

            withClue("a shared browser must not show the next person this address") {
                state.pendingActivation.shouldBeNull()
            }

            root.button("Send a new link").click()
            root.awaitText("A new link is on its way to me@test.com")
        }
    }

    "the next sign-in attempt clears a stale carrier, even when it fails" {
        val server = FakeAuthServer().apply {
            answer(signInPath, AuthSignInResponse.serializer()) {
                AuthSignInResponse.ActivationRequired(
                    realm = AuthRealmModel(id = RealmId("test"), providers = emptyList(), passwordPolicy = PasswordPolicy.default),
                    resendToken = "resend-token",
                )
            }
        }
        val state = stateOn(server)

        state.login(AuthSignInRequest.EmailAndPassword(provider = "email-password", email = "first@test.com", password = "pw"))
        state.pendingActivation.shouldNotBeNull()

        server.answers.remove(signInPath) // the next attempt fails in transport

        state.login(AuthSignInRequest.EmailAndPassword(provider = "email-password", email = "second@test.com", password = "pw"))
        state.pendingActivation.shouldBeNull()
    }

    "resend copy is honest: 'recently sent' ONLY when the server names the delay" {
        val outcomes = listOf(
            AuthResendActivationResponse(sent = false, nextResendAt = MpInstant.now().plus(10.minutes)) to
                    "We already sent a link to me@test.com recently",
            AuthResendActivationResponse(sent = false) to "We could not send a new link",
            null to "We could not reach the server",
        )

        outcomes.forEach { (answer, expected) ->
            withClue("server answers $answer") {
                val server = FakeAuthServer().apply {
                    answer(signInPath, AuthSignInResponse.serializer()) {
                        AuthSignInResponse.ActivationRequired(
                            realm = AuthRealmModel(
                                id = RealmId("test"), providers = emptyList(), passwordPolicy = PasswordPolicy.default,
                            ),
                            resendToken = "resend-token",
                        )
                    }
                    answer(resendPath, AuthResendActivationResponse.serializer()) { answer }
                }
                val state = stateOn(server)

                state.login(AuthSignInRequest.EmailAndPassword(provider = "email-password", email = "me@test.com", password = "pw"))

                TestBed.preact({ ActivateAccountPage(state = state, provider = "email-password", token = null) }) { root ->
                    root.button("Send a new link").click()
                    root.awaitText(expected)

                    if (answer?.nextResendAt == null) {
                        root.containsText("recently") shouldBe false
                    }
                }
            }
        }
    }
})
