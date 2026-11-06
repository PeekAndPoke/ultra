package io.peekandpoke.funktor.auth.widgets

import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.html.onClick
import io.peekandpoke.ultra.semanticui.icon
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.browser.window
import kotlinx.html.Tag
import kotlinx.html.div

@Suppress("FunctionName")
fun Tag.GithubSignInButton(
    clientId: String,
    label: String,
    callbackUrl: String,
    action: GithubOAuth.Action,
) = comp(
    GithubSignInButton.Props(
        clientId = clientId,
        label = label,
        callbackUrl = callbackUrl,
        action = action,
    )
) {
    GithubSignInButton(it)
}

class GithubSignInButton(ctx: Ctx<Props>) : Component<GithubSignInButton.Props>(ctx) {

    // https://medium.com/@rbscoop2611/how-to-add-login-with-github-functionality-in-your-react-project-using-oauth2-781e84247328

    //  PROPS  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class Props(
        val clientId: String,
        val label: String,
        /** Where GitHub sends the browser back: no fragment (RFC 6749 §3.1.2); the route rides in the state. */
        val callbackUrl: String,
        /** Sign in or up: rides in the stored state, so the callback cannot be told otherwise by its URL. */
        val action: GithubOAuth.Action,
    )

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        div {
            ui.fluid.basic.button {
                onClick {
                    // `user:email` lets the server read which addresses GitHub verified (`GET /user/emails`):
                    // without it every GitHub sign-in and sign-up is refused. `GithubOAuth` encodes every value and
                    // stores the state the callback must bring back (ULTRA-00288).
                    window.location.href = GithubOAuth.begin(
                        clientId = props.clientId,
                        redirectUri = props.callbackUrl,
                        action = props.action,
                        hash = window.location.hash,
                    )
                }

                icon.github()
                +props.label
            }
        }
    }
}
