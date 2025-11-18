package de.peekandpoke.funktor.auth.pages

import de.peekandpoke.funktor.auth.AuthState
import de.peekandpoke.funktor.auth.asFormRule
import de.peekandpoke.funktor.auth.model.AuthProviderModel
import de.peekandpoke.funktor.auth.model.AuthRecoverAccountRequest
import de.peekandpoke.funktor.auth.model.AuthRecoverAccountResponse
import de.peekandpoke.funktor.auth.model.AuthSignInRequest
import de.peekandpoke.funktor.auth.model.AuthSignUpRequest
import de.peekandpoke.funktor.auth.widgets.GithubSignInButton
import de.peekandpoke.funktor.auth.widgets.GoogleSignInButton
import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.forms.formController
import de.peekandpoke.kraft.forms.validation.strings.notEmpty
import de.peekandpoke.kraft.routing.Router.Companion.router
import de.peekandpoke.kraft.semanticui.forms.UiInputField
import de.peekandpoke.kraft.semanticui.forms.UiPasswordField
import de.peekandpoke.kraft.utils.dataLoader
import de.peekandpoke.kraft.utils.doubleClickProtection
import de.peekandpoke.kraft.utils.launch
import de.peekandpoke.ultra.common.model.Message
import de.peekandpoke.ultra.html.css
import de.peekandpoke.ultra.html.onClick
import de.peekandpoke.ultra.html.onSubmit
import de.peekandpoke.ultra.semanticui.icon
import de.peekandpoke.ultra.semanticui.noui
import de.peekandpoke.ultra.semanticui.ui
import kotlinx.browser.window
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.css.Cursor
import kotlinx.css.cursor
import kotlinx.html.FlowContent
import kotlinx.html.a
import kotlinx.html.div
import kotlinx.serialization.json.jsonPrimitive
import org.w3c.dom.url.URLSearchParams

class LoginController<USER>(
    private val host: Component<*>,
    private val state: AuthState<USER>,
) {
    companion object {
        const val AUTH_CALLBACK_PARAM = "auth-callback"
        const val AUTH_ACTION_PARAM = "auth-action"
    }

    sealed interface DisplayState {
        data class Login(
            val email: String = "",
            val password: String = "",
            val message: Message? = null,
        ) : DisplayState

        data class RecoverPassword(
            val email: String = "",
            val provider: AuthProviderModel,
            val message: Message? = null,
        ) : DisplayState

        // Sign-Up state holds all input data for the sign-up form
        data class SignUp(
            val provider: AuthProviderModel,
            val email: String = "",
            val displayName: String = "",
            val password: String = "",
            val message: Message? = null,
        ) : DisplayState

        fun withMessage(message: Message?) = when (this) {
            is Login -> copy(message = message)
            is RecoverPassword -> copy(message = message)
            is SignUp -> copy(message = message)
        }
    }

    inner class Renderer internal constructor() {
        operator fun <T> invoke(block: Renderer.() -> T): T = this.block()

        fun FlowContent.renderMessage(message: Message?) = when (message?.type) {
            null -> Unit
            Message.Type.info -> ui.info.message { +message.text }
            Message.Type.warning -> ui.warning.message { +message.text }
            Message.Type.error -> ui.error.message { +message.text }
        }

        fun FlowContent.renderBackLink(backState: DisplayState) {
            div {
                a {
                    css {
                        cursor = Cursor.pointer
                    }

                    onClick { evt ->
                        evt.preventDefault()
                        displayState = backState
                    }

                    icon.angle_left()
                    +"Back"
                }
            }
        }

        fun FlowContent.renderLoginState(s: DisplayState.Login) {
            val realm = realmLoader.value() ?: return

            renderMessage(s.message)

            ui.header { +"Sign In" }
            renderSignInProvidersAsList(s)

            if (realm.signUpProviders.isNotEmpty()) {
                ui.hidden.divider()

                ui.header { +"Sign Up" }

                renderSignUpProvidersAsList()
            }
        }

        fun FlowContent.renderRecoverPasswordState(s: DisplayState.RecoverPassword) {

            renderMessage(s.message)

            ui.form Form {
                onSubmit { evt ->
                    evt.preventDefault()
                }

                ui.header {
                    +"Enter your email to recover your password"
                }

                UiInputField(s.email, { displayState = s.copy(email = it) }) {
                    placeholder("Email")
                }

                ui.field {
                    ui.primary.fluid.givenNot(noDblClick.canRun) { loading }.button Submit {
                        onClick {
                            launch {
                                initPasswordReset(
                                    AuthRecoverAccountRequest.InitPasswordReset(
                                        provider = s.provider.id,
                                        email = s.email,
                                    )
                                )

                                // NOTICE: We always show this message no matter if the email address exists or not.
                                //         If we were to show an error if an email address does not exist, we would
                                //         expose sensitive data to an attacker.
                                displayState = DisplayState.Login(
                                    email = s.email,
                                    message = Message.info("Recovery email sent. Please check your inbox."),
                                )
                            }
                        }
                        +"Recover Password"
                    }
                }

                ui.hidden.divider {}

                renderBackLink(DisplayState.Login(email = s.email))
            }
        }

        fun FlowContent.renderSignUpState(s: DisplayState.SignUp) {
            val realm = realmLoader.value() ?: return

            renderMessage(s.message)

            ui.header { +"Create your account" }

            ui.form Form {
                onSubmit { evt ->
                    evt.preventDefault()
                    if (formCtrl.validate()) {
                        val req = AuthSignUpRequest.EmailAndPassword(
                            provider = s.provider.id,
                            email = s.email,
                            displayName = s.displayName.ifBlank { null },
                            password = s.password,
                        )
                        signup(req)
                    }
                }

                UiInputField(s.email, { displayState = s.copy(email = it) }) {
                    placeholder("Email")
                    accepts(
                        notEmpty(),
                    )
                }

                UiInputField(s.displayName, { displayState = s.copy(displayName = it) }) {
                    placeholder("Display name (optional)")
                }

                UiPasswordField(s.password, { displayState = s.copy(password = it) }) {
                    placeholder("Password")
                    revealPasswordIcon()
                    accepts(
                        realm.passwordPolicy.asFormRule()
                    )
                }

                ui.field {
                    ui.primary.fluid
                        .givenNot(noDblClick.canRun) { loading }
                        .givenNot(formCtrl.isValid) { disabled }
                        .button Submit {
                        +"Create account"
                    }
                }

                ui.hidden.divider()

                renderBackLink(DisplayState.Login(email = s.email))
            }
        }

        fun FlowContent.renderSignInProvidersAsList(s: DisplayState.Login) {
            val realm = realmLoader.value() ?: return
            val signInProviders = realm.signInProviders

            ui.list {
                signInProviders.forEach { provider ->
                    when (provider.type) {
                        AuthProviderModel.TYPE_EMAIL_PASSWORD -> noui.item {
                            renderEmailPasswordForm(s, provider)
                        }

                        AuthProviderModel.TYPE_GOOGLE -> noui.item {
                            renderGoogleSignInButton(provider)
                        }

                        AuthProviderModel.TYPE_GITHUB -> noui.item {
                            renderGithubSignInButton(provider)
                        }

                        else -> {
                            console.error("LoginController: Unsupported Login provider: ${provider.type}")
                        }
                    }
                }
            }
        }

        fun FlowContent.renderSignUpProvidersAsList() {
            val realm = realmLoader.value() ?: return
            val signUpProviders = realm.signUpProviders

            ui.list {
                signUpProviders.forEach { provider ->
                    when (provider.type) {
                        AuthProviderModel.TYPE_EMAIL_PASSWORD -> noui.item {
                            ui.basic.fluid.button {
                                onClick { displayState = DisplayState.SignUp(provider = provider) }
                                icon.mail()
                                +"Sign up with Email & Password"
                            }
                        }

                        AuthProviderModel.TYPE_GOOGLE -> noui.item {
                            renderGoogleSignUpButton(provider)
                        }

                        AuthProviderModel.TYPE_GITHUB -> noui.item {
                            renderGithubSignUpButton(provider)
                        }

                        else -> {
                            console.error("LoginController: Unsupported SignUp provider: ${provider.type}")
                        }
                    }
                }
            }
        }

        fun FlowContent.renderEmailPasswordForm(state: DisplayState.Login, provider: AuthProviderModel) {
            ui.form Form {
                onSubmit { evt ->
                    evt.preventDefault()
                }

                UiInputField(state.email, { displayState = state.copy(email = it) }) {
                    placeholder("Email")
                }

                UiPasswordField(state.password, { displayState = state.copy(password = it) }) {
                    placeholder("Password")
                    revealPasswordIcon()
                }

                ui.field {
                    ui.primary.fluid.givenNot(noDblClick.canRun) { loading }.button Submit {
                        onClick {
                            login(
                                AuthSignInRequest.EmailAndPassword(
                                    provider = provider.id,
                                    email = state.email,
                                    password = state.password,
                                )
                            )
                        }
                        +"Login"
                    }
                }

                ui.field {
                    a {
                        onClick { evt ->
                            evt.preventDefault()
                            displayState = DisplayState.RecoverPassword(
                                email = state.email,
                                provider = provider,
                            )
                        }
                        +"Forgot Password?"
                    }
                }
            }

            ui.hidden.divider()
        }

        fun FlowContent.renderGoogleSignInButton(provider: AuthProviderModel) {
            val clientId = provider.config?.get("client-id")?.jsonPrimitive?.content ?: ""

            GoogleSignInButton(clientId = clientId) { token ->
                login(
                    AuthSignInRequest.OAuth(provider = provider.id, token = token)
                )
            }
        }

        fun FlowContent.renderGoogleSignUpButton(provider: AuthProviderModel) {
            val clientId = provider.config?.get("client-id")?.jsonPrimitive?.content ?: ""

            GoogleSignInButton(
                clientId = clientId,
                text = "signup_with",
                theme = "outline",
                shape = "rectangular",
                size = "large",
                logoAlignment = "center",
                fullWidth = true,
            ) { token ->
                signup(
                    AuthSignUpRequest.OAuth(provider = provider.id, token = token)
                )
            }
        }

        fun FlowContent.renderGithubSignInButton(provider: AuthProviderModel) {
            val clientId = provider.config?.get("client-id")?.jsonPrimitive?.content ?: ""

            val parts = listOf(
                window.location.origin,
                window.location.pathname,
                "?${AUTH_CALLBACK_PARAM}=${provider.id}",
                window.location.hash,
            )

            val callbackUrl = parts.joinToString("")

            GithubSignInButton(
                clientId = clientId,
                label = "Sign in with GitHub",
                callbackUrl = callbackUrl,
            ) { token ->
                login(
                    AuthSignInRequest.OAuth(provider = provider.id, token = token)
                )
            }
        }

        fun FlowContent.renderGithubSignUpButton(provider: AuthProviderModel) {
            val clientId = provider.config?.get("client-id")?.jsonPrimitive?.content ?: ""

            val parts = listOf(
                window.location.origin,
                window.location.pathname,
                "?${AUTH_CALLBACK_PARAM}=${provider.id}&auth-action=signup",
                window.location.hash,
            )

            val callbackUrl = parts.joinToString("")

            GithubSignInButton(
                clientId = clientId,
                label = "Sign up with GitHub",
                callbackUrl = callbackUrl,
            ) { token ->
                signup(
                    AuthSignUpRequest.OAuth(provider = provider.id, token = token)
                )
            }
        }
    }

    val renderer: Renderer = Renderer()

    val realmLoader = host.dataLoader {
        state.api.getRealm().map { it.data!! }
    }

    var displayState: DisplayState by host.value(DisplayState.Login())

    val formCtrl = host.formController()
    val noDblClick = host.doubleClickProtection()

    fun handleAuthCallback() {
        val realm = realmLoader.value() ?: return

        // Handle any callback params ... f.e. from Github-OAuth
        val params = URLSearchParams(window.location.search)

        if (params.has(AUTH_CALLBACK_PARAM)) {
            val providerId = params.get(AUTH_CALLBACK_PARAM)
            val action = params.get(AUTH_ACTION_PARAM)
            val provider = realm.providers.find { it.id == providerId }

            when (provider?.type) {
                AuthProviderModel.TYPE_GITHUB -> {
                    params.get("code")?.let { code ->
                        if (action == "signup") {
                            signup(AuthSignUpRequest.OAuth(provider = provider.id, token = code))
                        } else {
                            login(AuthSignInRequest.OAuth(provider = provider.id, token = code))
                        }
                    }
                }
            }

            // Remove the excess query params from the uri
            val parts = listOf(window.location.origin, window.location.pathname, window.location.hash)

            window.history.pushState(null, "", parts.joinToString(""))
        }
    }

    fun login(request: AuthSignInRequest) {
        launch {
            doLogin(request)
        }
    }

    suspend fun initPasswordReset(
        request: AuthRecoverAccountRequest.InitPasswordReset,
    ): AuthRecoverAccountResponse.InitPasswordReset? {
        return noDblClick.runBlocking {
            state.recoverAccountInitPasswordReset(request)
        }
    }

    fun signup(request: AuthSignUpRequest) {
        launch {
            doSignup(request)
        }
    }

    private suspend fun doLogin(request: AuthSignInRequest) = noDblClick.runBlocking {
        displayState = displayState.withMessage(message = null)

        val result = state.login(request)

        if (result.isLoggedIn) {
            val uri = host.router.strategy.render(state.frontend.config.redirectAfterLogin)
            console.info("Login success. Redirecting to $uri")
            state.redirectAfterLogin(uri)
        } else {
            displayState = displayState.withMessage(Message.error("Login failed"))
        }
    }


    private suspend fun doSignup(request: AuthSignUpRequest) = noDblClick.runBlocking {
        // Clear any previous message
        displayState = displayState.withMessage(message = null)

        val response = state.api.signUp(request)
            .catch { /* noop */ }
            .map { it }
            .firstOrNull()

        val data = response?.data

        displayState = when {
            data?.success == true -> {
                val emailPrefill = when (request) {
                    is AuthSignUpRequest.EmailAndPassword -> request.email
                    else -> ""
                }
                DisplayState.Login(
                    email = emailPrefill,
                    message = if (data.requiresActivation) {
                        Message.info("Account created. Please check your email to activate your account.")
                    } else {
                        Message.info("Account created. You can now sign in.")
                    }
                )
            }

            else -> {
                displayState.withMessage(Message.error("Sign-up failed"))
            }
        }
    }
}
