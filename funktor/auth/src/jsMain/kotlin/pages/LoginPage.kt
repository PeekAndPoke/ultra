package de.peekandpoke.funktor.auth.pages

import de.peekandpoke.funktor.auth.AuthState
import de.peekandpoke.funktor.auth.pages.LoginController.DisplayState
import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.components.Ctx
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.vdom.VDom
import de.peekandpoke.ultra.html.onClick
import de.peekandpoke.ultra.semanticui.ui
import kotlinx.html.FlowContent
import kotlinx.html.Tag

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
        ctrl.realmLoader.value {
            ctrl.handleAuthCallback()
        }
    }

    override fun VDom.render() {
        AuthPageLayouts {
            renderFullscreenBackgroundLayout(authState.frontend.config) {
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

            loaded {
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
