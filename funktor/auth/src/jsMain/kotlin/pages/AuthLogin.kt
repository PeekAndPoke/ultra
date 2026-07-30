package io.peekandpoke.funktor.auth.pages

import io.peekandpoke.funktor.auth.AuthState
import io.peekandpoke.funktor.auth.pages.LoginController.DisplayState
import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.html.onClick
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.html.Tag

/**
 * The shared, self-contained auth widget: the [LoginController] state machine plus the login /
 * sign-up / password-recovery / org-selection snippets. Renders **only** the auth card — no page
 * chrome — so an app can embed it inside its own branded layout:
 *
 * ```
 * mount(Nav.auth.login) {
 *     MyLoggedOutLayout { AuthLogin(State.auth) }
 * }
 * ```
 *
 * [LoginPage] is the batteries-included default that wraps this in the fullscreen-background chrome.
 */
@Suppress("FunctionName")
fun <USER> Tag.AuthLogin(
    state: AuthState<USER>,
) = comp(
    AuthLogin.Props(state = state)
) {
    AuthLogin(it)
}

class AuthLogin<USER>(ctx: Ctx<Props<USER>>) : Component<AuthLogin.Props<USER>>(ctx) {

    data class Props<USER>(
        val state: AuthState<USER>,
    )

    private val ctrl = LoginController(this, props.state)

    init {
        ctrl.realmLoader.value {
            ctrl.handleAuthCallback()
        }
    }

    override fun VDom.render() {
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
                        is DisplayState.SelectOrg -> renderSelectOrgState(s)
                    }
                }
            }
        }
    }
}
