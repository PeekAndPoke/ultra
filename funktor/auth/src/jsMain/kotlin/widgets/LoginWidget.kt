@file:Suppress("detekt:LongMethod")

package de.peekandpoke.funktor.auth.widgets

import de.peekandpoke.funktor.auth.AuthState
import de.peekandpoke.funktor.auth.asFormRule
import de.peekandpoke.funktor.auth.model.AuthProviderModel
import de.peekandpoke.funktor.auth.model.AuthRealmModel
import de.peekandpoke.funktor.auth.model.AuthRecoveryRequest
import de.peekandpoke.funktor.auth.model.AuthRecoveryResponse
import de.peekandpoke.funktor.auth.model.AuthSignInRequest
import de.peekandpoke.funktor.auth.model.AuthSignUpRequest
import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.components.Ctx
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.forms.formController
import de.peekandpoke.kraft.forms.validation.strings.notEmpty
import de.peekandpoke.kraft.routing.Route
import de.peekandpoke.kraft.routing.Router.Companion.router
import de.peekandpoke.kraft.semanticui.forms.UiInputField
import de.peekandpoke.kraft.semanticui.forms.UiPasswordField
import de.peekandpoke.kraft.utils.dataLoader
import de.peekandpoke.kraft.utils.doubleClickProtection
import de.peekandpoke.kraft.utils.launch
import de.peekandpoke.kraft.vdom.VDom
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
import kotlinx.html.Tag
import kotlinx.html.a
import kotlinx.html.div
import kotlinx.serialization.json.jsonPrimitive
import org.w3c.dom.url.URLSearchParams

@Suppress("FunctionName")
fun <USER> Tag.LoginWidget(
    state: AuthState<USER>,
    onLoginSuccessUri: Route.Bound,
) = comp(
    LoginWidget.Props(
        state = state,
        onLoginSuccessUri = onLoginSuccessUri,
    )
) {
    LoginWidget(it)
}

class LoginWidget<USER>(ctx: Ctx<Props<USER>>) : Component<LoginWidget.Props<USER>>(ctx) {

    companion object {
        const val AUTH_CALLBACK_PARAM = "auth-callback"
    }

    //  PROPS  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class Props<USER>(
        val state: AuthState<USER>,
        val onLoginSuccessUri: Route.Bound,
    )

    private sealed interface DisplayState {
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

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private var displayState: DisplayState by value(DisplayState.Login())

    val realmLoader = dataLoader {
        props.state.api.getRealm().map { it.data!! }
    }

    private val formCtrl = formController()
    private val noDblClick = doubleClickProtection()

    init {
        realmLoader.value { realm ->
            realm?.let {
                // Handle any callback params ... f.e. from Github-OAuth
                val params = URLSearchParams(window.location.search)

                if (params.has(AUTH_CALLBACK_PARAM)) {
                    val providerId = params.get(AUTH_CALLBACK_PARAM)
                    val provider = realm.providers.find { it.id == providerId }
                    val action = params.get("auth-action")

                    when (provider?.type) {
                        AuthProviderModel.TYPE_GITHUB -> {
                            params.get("code")?.let { code ->
                                if (action == "signup") {
                                    signup(
                                        AuthSignUpRequest.OAuth(provider = provider.id, token = code)
                                    )
                                } else {
                                    login(
                                        AuthSignInRequest.OAuth(provider = provider.id, token = code)
                                    )
                                }
                            }
                        }
                    }

                    // Remove the excess query params from the uri
                    val parts = listOf(
                        window.location.origin,
                        window.location.pathname,
                        window.location.hash
                    )

                    window.history.pushState(null, "", parts.joinToString(""))
                }
            }
        }
    }

    private fun login(request: AuthSignInRequest) {
        launch {
            doLogin(request)
        }
    }

    private suspend fun doLogin(request: AuthSignInRequest) = noDblClick.runBlocking {
        displayState = displayState.withMessage(message = null)

        val result = props.state.login(request)

        if (result.isLoggedIn) {
            val uri = router.strategy.render(props.onLoginSuccessUri)
            console.log("Login success. Redirecting to $uri")
            props.state.redirectAfterLogin(uri)
        } else {
            displayState = displayState
                .withMessage(message = Message.error("Login failed"))
        }
    }

    private suspend fun recover(request: AuthRecoveryRequest): AuthRecoveryResponse? {
        return noDblClick.runBlocking {
            props.state.recover(request)
        }
    }

    private fun signup(request: AuthSignUpRequest) {
        launch {
            doSignup(request)
        }
    }

    private suspend fun doSignup(request: AuthSignUpRequest) = noDblClick.runBlocking {
        // Clear any previous message
        displayState = displayState.withMessage(message = null)

        val response = props.state.api
            .signUp(request)
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

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {

        realmLoader(this) {
            loading {
                ui.basic.loading.segment {
                }
            }

            error {
                ui.basic.segment {
                    onClick { realmLoader.reload() }
                    +"Login not possible. Please try again later."
                }
            }

            loaded { realm ->
                when (val s = displayState) {
                    is DisplayState.Login -> renderLoginState(s, realm)
                    is DisplayState.RecoverPassword -> renderRecoverPasswordState(s)
                    is DisplayState.SignUp -> renderSignUpState(s, realm)
                }
            }
        }
    }

    private fun FlowContent.renderMessage(message: Message?) = when (message?.type) {
        null -> Unit
        Message.Type.info -> ui.info.message { +message.text }
        Message.Type.warning -> ui.warning.message { +message.text }
        Message.Type.error -> ui.error.message { +message.text }
    }

    private fun FlowContent.renderLoginState(state: DisplayState.Login, realm: AuthRealmModel) {

        renderMessage(state.message)

        val signInProviders = realm.signInProviders
        val signUpProviders = realm.signUpProviders

        val numColumns = listOf(
            signInProviders.isNotEmpty(),
            signUpProviders.isNotEmpty(),
        ).count { it }

        console.log("Num columns", numColumns)

        ui.number(numColumns).column.stackable.divided.grid {
            if (signInProviders.isNotEmpty()) {
                noui.column {
                    ui.header { +"Sign In" }

                    ui.list {
                        signInProviders.forEach { provider ->
                            when (provider.type) {
                                AuthProviderModel.TYPE_EMAIL_PASSWORD -> noui.item {
                                    renderEmailPasswordForm(state, provider)
                                }

                                AuthProviderModel.TYPE_GOOGLE -> noui.item {
                                    renderGoogleSignInButton(provider)
                                }

                                AuthProviderModel.TYPE_GITHUB -> noui.item {
                                    renderGithubSignInButton(provider)
                                }

                                else -> {
                                    console.warn("LoginWidget: Unsupported login provider type: ${provider.type}")
                                }
                            }
                        }
                    }
                }
            }

            if (signUpProviders.isNotEmpty()) {
                noui.column {
                    ui.header { +"Sign Up" }

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

                                else -> noui.item {
                                    ui.message { +"Sign up available for provider ${provider.type}" }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun FlowContent.renderEmailPasswordForm(state: DisplayState.Login, provider: AuthProviderModel) {
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
                ui.orange.fluid.givenNot(noDblClick.canRun) { loading }.button Submit {
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

    private fun FlowContent.renderGoogleSignInButton(provider: AuthProviderModel) {
        val clientId = provider.config?.get("client-id")?.jsonPrimitive?.content ?: ""

        GoogleSignInButton(clientId = clientId) { token ->
            login(
                AuthSignInRequest.OAuth(provider = provider.id, token = token)
            )
        }
    }

    private fun FlowContent.renderGoogleSignUpButton(provider: AuthProviderModel) {
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

    private fun FlowContent.renderGithubSignInButton(provider: AuthProviderModel) {
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

    private fun FlowContent.renderGithubSignUpButton(provider: AuthProviderModel) {
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

    private fun FlowContent.renderRecoverPasswordState(state: DisplayState.RecoverPassword) {

        renderMessage(state.message)

        ui.form Form {
            onSubmit { evt ->
                evt.preventDefault()
            }

            ui.header {
                +"Enter your email to recover your password"
            }

            UiInputField(state.email, { displayState = state.copy(email = it) }) {
                placeholder("Email")
            }

            ui.field {
                ui.orange.fluid.givenNot(noDblClick.canRun) { loading }.button Submit {
                    onClick {
                        launch {
                            val result = recover(
                                AuthRecoveryRequest.ResetPassword(
                                    provider = state.provider.id,
                                    email = state.email,
                                )
                            )

                            displayState = when (result?.success) {
                                true -> DisplayState.Login(
                                    email = state.email,
                                    message = Message.info("Recovery email sent. Please check your inbox."),
                                )

                                else -> state.copy(message = Message.error("Recovery not possible"))
                            }
                        }
                    }
                    +"Recover Password"
                }
            }
        }

        ui.hidden.divider {}

        renderBack(DisplayState.Login(email = state.email))
    }

    // NEW: render sign-up state with validators and a single password field
    private fun FlowContent.renderSignUpState(state: DisplayState.SignUp, realm: AuthRealmModel) {
        renderMessage(state.message)

        ui.header { +"Create your account" }

        ui.form Form {
            onSubmit { evt ->
                evt.preventDefault()
                if (formCtrl.validate()) {
                    val req = AuthSignUpRequest.EmailAndPassword(
                        provider = state.provider.id,
                        email = state.email,
                        displayName = state.displayName.ifBlank { null },
                        password = state.password,
                    )
                    signup(req)
                }
            }

            UiInputField(state.email, { displayState = state.copy(email = it) }) {
                placeholder("Email")
                accepts(
                    notEmpty(),
                )
            }

            UiInputField(state.displayName, { displayState = state.copy(displayName = it) }) {
                placeholder("Display name (optional)")
            }

            UiPasswordField(state.password, { displayState = state.copy(password = it) }) {
                placeholder("Password")
                revealPasswordIcon()
                accepts(
                    realm.passwordPolicy.asFormRule()
                )
            }

            ui.field {
                ui.green.fluid
                    .givenNot(noDblClick.canRun) { loading }
                    .givenNot(formCtrl.isValid) { disabled }
                    .button Submit {
                    +"Create account"
                }
            }

            ui.hidden.divider()

            renderBack(DisplayState.Login(email = state.email))
        }
    }

    private fun FlowContent.renderBack(state: DisplayState) {
        div {
            a {
                css {
                    cursor = Cursor.pointer
                }

                onClick { evt ->
                    evt.preventDefault()
                    displayState = state
                }

                icon.angle_left()
                +"Back"
            }
        }
    }
}
