package de.peekandpoke.funktor.auth.pages

import de.peekandpoke.funktor.auth.AuthState
import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.components.Ctx
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.vdom.VDom
import de.peekandpoke.ultra.semanticui.ui
import kotlinx.html.Tag

@Suppress("FunctionName")
fun <USER> Tag.ResetPasswordPage(
    state: AuthState<USER>,
    realm: String,
    token: String,
) = comp(
    ResetPasswordPage.Props(
        state = state,
        realm = realm,
        token = token,
    )
) {
    ResetPasswordPage(it)
}

class ResetPasswordPage<USER>(ctx: Ctx<Props<USER>>) : Component<ResetPasswordPage.Props<USER>>(ctx) {

    //  PROPS  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class Props<USER>(
        val state: AuthState<USER>,
        val realm: String,
        val token: String,
    )

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    val state get() = props.state

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        AuthPageLayouts {
            renderFullscreenBackgroundLayout(state.config) {
                ui.header { +"Reset Password" }
            }
        }
    }
}
