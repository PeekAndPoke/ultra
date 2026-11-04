package io.peekandpoke.funktor.auth.pages

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
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
import io.peekandpoke.funktor.auth.model.AuthActivateAccountResponse
import io.peekandpoke.funktor.auth.model.AuthCancelSignUpResponse
import io.peekandpoke.funktor.auth.model.AuthRealmModel
import io.peekandpoke.funktor.auth.model.AuthSignInResponse
import io.peekandpoke.funktor.auth.model.PasswordPolicy
import io.peekandpoke.funktor.auth.model.RealmId
import io.peekandpoke.kraft.routing.Router
import io.peekandpoke.kraft.routing.RouterComponent
import io.peekandpoke.kraft.routing.Static
import io.peekandpoke.kraft.routing.router
import io.peekandpoke.kraft.testing.KQuery
import io.peekandpoke.kraft.testing.TestBed
import io.peekandpoke.kraft.testing.click
import io.peekandpoke.kraft.testing.containsText
import io.peekandpoke.kraft.testing.selectCss
import io.peekandpoke.kraft.testing.typeText
import io.peekandpoke.ultra.remote.ApiClient
import io.peekandpoke.ultra.remote.ApiResponse
import io.peekandpoke.ultra.remote.api
import io.peekandpoke.ultra.security.user.UserId
import io.peekandpoke.ultra.security.user.UserPermissions
import kotlinx.browser.window
import kotlinx.coroutines.delay
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.w3c.dom.Element

/**
 * The activation page in a real browser, against a fake auth server (a ktor MockEngine — the server is the
 * one thing a browser test cannot run).
 *
 * The property that matters most: **opening a link from the mail POSTs NOTHING.** Mail gateways open links
 * in a sandbox, so a page that activated on load would activate accounts nobody chose to. The page only reads the
 * realm, for its password policy.
 */
class ActivateAccountPageSpec : StringSpec({

    val codec = Json { ignoreUnknownKeys = true }

    /** Answers one auth endpoint; `null` makes the request fail in transport. */
    class FakeAuthServer {
        val calls = mutableListOf<String>()
        val bodies = mutableListOf<JsonObject>()
        val answers = mutableMapOf<String, () -> String?>()

        fun <T> answer(path: String, serializer: KSerializer<T>, value: () -> T?) {
            answers[path] = { value()?.let { codec.encodeToString(serializer.api(), ApiResponse.ok(it)) } }
        }

        val client = HttpClient(
            MockEngine { request ->
                val path = request.url.encodedPath
                calls += "${request.method.value} $path"
                (request.body as? TextContent)?.let { bodies += codec.parseToJsonElement(it.text).jsonObject }

                val body = answers[path]?.invoke() ?: error("the server cannot be reached")

                respond(content = body, status = HttpStatusCode.OK, headers = headersOf(HttpHeaders.ContentType, "application/json"))
            }
        )
    }

    fun stateOn(server: FakeAuthServer, router: () -> Router = { error("this spec does not navigate") }) = AuthState(
        userSerializer = JsonObject.serializer(),
        frontend = AuthFrontend.default(config = AuthFrontendConfig(redirectAfterLogin = Static("/")())),
        api = AuthApiClient(
            realm = RealmId("test"),
            config = ApiClient.Config(baseUrl = "http://auth.test", codec = codec, client = server.client),
        ),
        router = router,
        sessionConfig = AuthSessionConfig(enabled = false),
    )

    suspend fun KQuery<Element>.button(text: String): KQuery<Element> =
        KQuery(selectCss(".button").filter { it.textContent?.contains(text) == true })

    suspend fun KQuery<Element>.awaitText(text: String) {
        repeat(40) {
            if (containsText(text)) return
            delay(50)
        }
        withClue("expected the page to show '$text', it shows: ${firstOrNull()?.textContent}") { containsText(text) shouldBe true }
    }

    val realmPath = "/auth/test/realm"
    val activatePath = "/auth/test/activate"
    val cancelPath = "/auth/test/activate/cancel"
    val goodPassword = "New-1234!"

    val realmModel = AuthRealmModel(id = RealmId("test"), providers = emptyList(), passwordPolicy = PasswordPolicy.default)

    /** A fake server that serves the realm, so the page can show its password field. */
    fun serverWithRealm() = FakeAuthServer().apply {
        answer(realmPath, AuthRealmModel.serializer()) { realmModel }
    }

    fun FakeAuthServer.posts() = calls.filter { it.startsWith("POST") }

    suspend fun KQuery<Element>.choosePassword(password: String) {
        awaitText("Choose a password")
        selectCss("input[type=password]").typeText(password)
        delay(50)
        button("Activate my account").click()
    }

    "opening the activation link POSTs NOTHING — only choosing the password does, and it sends the password" {
        val server = serverWithRealm().apply {
            answer(activatePath, AuthActivateAccountResponse.serializer()) { AuthActivateAccountResponse(success = true) }
        }
        val state = stateOn(server)

        TestBed.preact({ ActivateAccountPage(state = state, provider = "email-password", token = "tok") }) { root ->
            // Give a load-time request every chance to happen.
            delay(300)

            withClue("a scanner that only OPENS the link must change nothing — reading the realm changes nothing") {
                server.calls shouldBe listOf("GET $realmPath")
            }

            root.button("cancel this account").size shouldBe 1

            root.choosePassword(goodPassword)
            root.awaitText("Your account is activated")

            server.posts() shouldBe listOf("POST $activatePath")
            server.bodies.single().let {
                it["token"]?.jsonPrimitive?.content shouldBe "tok"
                it["provider"]?.jsonPrimitive?.content shouldBe "email-password"
                it["password"]?.jsonPrimitive?.content shouldBe goodPassword
            }
        }
    }

    "a password that breaks the realm's policy is never sent" {
        val server = serverWithRealm()
        val state = stateOn(server)

        TestBed.preact({ ActivateAccountPage(state = state, provider = "email-password", token = "tok") }) { root ->
            root.choosePassword("weak")
            delay(300)

            server.posts() shouldBe emptyList()
        }
    }

    "a session in the answer signs the user in and leaves the page" {
        val server = serverWithRealm().apply {
            answer(activatePath, AuthActivateAccountResponse.serializer()) {
                AuthActivateAccountResponse(
                    success = true,
                    signIn = AuthSignInResponse.Success(
                        session = AuthSignInResponse.Session.Bearer("any.token.here"),
                        permissions = UserPermissions(),
                        userId = UserId("users/u1"),
                        realm = realmModel,
                        user = JsonObject(emptyMap()),
                    ),
                )
            }
        }
        lateinit var routes: Router
        val state = stateOn(server, router = { routes })
        routes = router { state.mount(this) }
        val originalHash = window.location.hash

        try {
            TestBed.preact({ RouterComponent(routes) }) { root ->
                routes.navToUri("/auth/email-password/activate/tok")
                root.choosePassword(goodPassword)

                repeat(40) { if (window.location.hash != "#/auth/email-password/activate/tok") return@repeat; delay(50) }

                state().isLoggedIn shouldBe true
                withClue("off to the app's redirect-after-login route") {
                    window.location.hash shouldBe "#/"
                }
            }
        } finally {
            // The session is persisted; the specs after this one must start signed out.
            state.logout()
            routes.disable()
            window.location.hash = originalHash
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
            server.posts() shouldBe emptyList()

            withClue("the cancel token cannot activate, so Cancel mode offers NO activate action — it points at the mail") {
                root.button("Activate").size shouldBe 0
                root.selectCss("input[type=password]").size shouldBe 0
                root.containsText("\"Choose your password\" link in the same mail") shouldBe true
            }

            root.button("Cancel this sign-up").click()
            root.awaitText("The sign-up is cancelled")

            server.posts() shouldBe listOf("POST $cancelPath")
            server.bodies.single()["token"]?.jsonPrimitive?.content shouldBe "tok"
        }
    }

    "a link the server refuses says so, and points at signing up again" {
        val server = serverWithRealm().apply {
            answer(activatePath, AuthActivateAccountResponse.serializer()) { AuthActivateAccountResponse(success = false) }
        }
        val state = stateOn(server)

        TestBed.preact({ ActivateAccountPage(state = state, provider = "email-password", token = "tok") }) { root ->
            root.choosePassword(goodPassword)
            root.awaitText("This link is not valid any more")
            root.containsText("sign up again with the same address") shouldBe true
        }
    }

    "the ACTIVATION page's cancel button sends the activation token to cancel — never to activate" {
        val server = serverWithRealm().apply {
            answer(cancelPath, AuthCancelSignUpResponse.serializer()) { AuthCancelSignUpResponse(success = true) }
        }
        val state = stateOn(server)

        TestBed.preact({ ActivateAccountPage(state = state, provider = "email-password", token = "activation-tok") }) { root ->
            root.button("cancel this account").click()
            root.awaitText("The sign-up is cancelled")

            server.posts() shouldBe listOf("POST $cancelPath")
            server.bodies.single()["token"]?.jsonPrimitive?.content shouldBe "activation-tok"
        }
    }

    "a refused CANCEL link speaks to someone who did not sign up — not 'try signing in'" {
        val server = FakeAuthServer().apply {
            answer(cancelPath, AuthCancelSignUpResponse.serializer()) { AuthCancelSignUpResponse(success = false) }
        }
        val state = stateOn(server)

        TestBed.preact({
            ActivateAccountPage(state = state, provider = "email-password", token = "tok", mode = ActivateAccountPage.Mode.Cancel)
        }) { root ->
            root.button("Cancel this sign-up").click()
            root.awaitText("Forgot password")
            root.containsText("Try signing in") shouldBe false
        }
    }

    "a server that cannot be reached is NOT reported as a dead link, and Retry repeats the click" {
        // The dead Retry branch of round 2: a transient failure on a valid link used to read "not valid".
        var reachable = false
        val server = serverWithRealm().apply {
            answer(activatePath, AuthActivateAccountResponse.serializer()) {
                AuthActivateAccountResponse(success = true).takeIf { reachable }
            }
        }
        val state = stateOn(server)

        TestBed.preact({ ActivateAccountPage(state = state, provider = "email-password", token = "tok") }) { root ->
            root.choosePassword(goodPassword)
            root.awaitText("We could not reach the server")
            root.containsText("not valid any more") shouldBe false

            reachable = true
            root.button("Retry").click()
            root.awaitText("Your account is activated")

            server.posts() shouldBe listOf("POST $activatePath", "POST $activatePath")
            withClue("the retry sends the same password") {
                server.bodies.map { it["password"]?.jsonPrimitive?.content } shouldBe listOf(goodPassword, goodPassword)
            }
        }
    }

    "each mailed deep link, MOUNTED through AuthState, opens its own mode and POSTs the token from the URL" {
        // The wiring `AuthFrontend.mount` owns: the activation link must open Activate-first, the cancel link
        // Cancel-first, and both must hand the URL's token — not some other value — to the server.
        val cases = listOf(
            Triple("/auth/email-password/activate/tok-a", "Activate my account", activatePath),
            Triple("/auth/email-password/cancel-signup/tok-c", "Cancel this sign-up", cancelPath),
        )

        cases.forEach { (uri, primary, endpoint) ->
            withClue(uri) {
                val server = serverWithRealm().apply {
                    answer(activatePath, AuthActivateAccountResponse.serializer()) { AuthActivateAccountResponse(success = true) }
                    answer(cancelPath, AuthCancelSignUpResponse.serializer()) { AuthCancelSignUpResponse(success = true) }
                }
                val state = stateOn(server)
                val routes = router { state.mount(this) }
                val originalHash = window.location.hash

                try {
                    TestBed.preact({ RouterComponent(routes) }) { root ->
                        routes.navToUri(uri)
                        root.awaitText(primary)
                        delay(300)
                        server.posts() shouldBe emptyList()

                        if (endpoint == activatePath) root.choosePassword(goodPassword) else root.button(primary).click()
                        root.awaitText("The sign-up is cancelled".takeIf { endpoint == cancelPath } ?: "Your account is activated")

                        server.posts() shouldBe listOf("POST $endpoint")
                        server.bodies.single().let {
                            it["token"]?.jsonPrimitive?.content shouldBe uri.substringAfterLast("/")
                            it["provider"]?.jsonPrimitive?.content shouldBe "email-password"
                        }
                    }
                } finally {
                    routes.disable()
                    window.location.hash = originalHash
                }
            }
        }
    }
})
