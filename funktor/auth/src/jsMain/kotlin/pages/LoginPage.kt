package de.peekandpoke.funktor.auth.pages

import de.peekandpoke.funktor.auth.AuthState
import de.peekandpoke.funktor.auth.model.AuthProviderModel
import de.peekandpoke.funktor.auth.model.AuthSignInRequest
import de.peekandpoke.funktor.auth.model.AuthSignUpRequest
import de.peekandpoke.funktor.auth.pages.LoginController.DisplayState
import de.peekandpoke.funktor.auth.widgets.LoginWidget.Companion.AUTH_CALLBACK_PARAM
import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.components.Ctx
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.vdom.VDom
import de.peekandpoke.ultra.html.onClick
import de.peekandpoke.ultra.semanticui.ui
import kotlinx.browser.window
import kotlinx.html.FlowContent
import kotlinx.html.Tag
import org.w3c.dom.url.URLSearchParams

@Suppress("FunctionName")
fun <USER> Tag.LoginPage(
    state: AuthState<USER>,
) = comp(
    LoginPage.Props(state = state)
) {
    LoginPage(it)
}

class LoginPage<USER>(ctx: Ctx<Props<USER>>) : Component<LoginPage.Props<USER>>(ctx) {

    //  PROPS  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class Props<USER>(
        val state: AuthState<USER>,
    )

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private val ctrl = LoginController(this, props.state)
    private val authState get() = props.state

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    init {
        ctrl.realmLoader.value { realm ->
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
                                    ctrl.signup(
                                        AuthSignUpRequest.OAuth(provider = provider.id, token = code)
                                    )
                                } else {
                                    ctrl.login(
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

    override fun VDom.render() {
        AuthPageLayouts {
            renderFullscreenBackgroundLayout(authState.config) {
                renderContent()
            }
        }
    }

    private fun FlowContent.renderContent() {
        ctrl.realmLoader(this) {
            loading {
                ui.basic.loading.segment {
                }
            }

            error {
                ui.basic.segment {
                    onClick { ctrl.realmLoader.reload() }
                    +"Login not possible. Please try again later."
                }
            }

            loaded { realm ->
                ctrl.renderer {
                    when (val s = ctrl.displayState) {
                        is DisplayState.Login -> renderLoginState(s)
                        is DisplayState.RecoverPassword -> renderRecoverPasswordState(s)
                        is DisplayState.SignUp -> renderSignUpState(s)
                    }
                }
            }
        }
    }
}
