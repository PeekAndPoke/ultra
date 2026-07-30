package io.peekandpoke.funktor.auth.pages

import io.peekandpoke.funktor.auth.AuthState
import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.vdom.VDom
import kotlinx.html.Tag

/**
 * Batteries-included default login page: the fullscreen-background chrome + optional branding from
 * [AuthFrontendConfig], wrapping the shared [AuthLogin] widget. Apps that want full control over the
 * chrome should embed [AuthLogin] in their own layout instead of mounting this.
 */
@Suppress("FunctionName")
fun <USER> Tag.LoginPage(
    state: AuthState<USER>,
) = comp(
    LoginPage.Props(state = state)
) {
    LoginPage(it)
}

class LoginPage<USER>(ctx: Ctx<Props<USER>>) : Component<LoginPage.Props<USER>>(ctx) {

    data class Props<USER>(
        val state: AuthState<USER>,
    )

    private val config get() = props.state.frontend.config

    override fun VDom.render() {
        AuthPageLayouts {
            renderFullscreenBackgroundLayout(config) {
                renderBranding(config)

                AuthLogin(props.state)
            }
        }
    }
}
