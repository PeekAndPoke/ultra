package io.peekandpoke.funktor.auth.pages

import io.peekandpoke.funktor.auth.AuthState
import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.html.FlowContent
import kotlinx.html.Tag
import kotlinx.html.img

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

    private fun FlowContent.renderBranding(config: AuthFrontendConfig) {
        // A custom header slot fully replaces the default logo + title.
        config.header?.let { slot ->
            slot()
            return
        }

        config.logoUrl?.let { url ->
            img(src = url, classes = "ui centered image") {}
        }

        config.title?.let { title ->
            ui.header { +title }
        }
    }
}
