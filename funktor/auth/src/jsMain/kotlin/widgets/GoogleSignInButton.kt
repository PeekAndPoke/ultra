package de.peekandpoke.funktor.auth.widgets

import com.benasher44.uuid.uuid4
import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.components.Ctx
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.vdom.VDom
import de.peekandpoke.ultra.html.data
import de.peekandpoke.ultra.semanticui.noui
import kotlinx.browser.window
import kotlinx.html.Tag
import kotlinx.html.classes
import kotlinx.html.div
import kotlinx.html.id
import kotlinx.html.script

@Suppress("FunctionName")
fun Tag.GoogleSignInButton(
    clientId: String,
    // Customization options (optional)
    text: String = "signin_with", // signin_with | signup_with | continue_with | signin
    theme: String = "outline", // outline | filled_blue | filled_black
    shape: String = "rectangular", // rectangular | pill | circle | square
    size: String = "large", // large | medium | small
    logoAlignment: String = "center", // left | center
    fullWidth: Boolean = true,
    onToken: (token: String) -> Unit,
) = comp(
    GoogleSignInButton.Props(
        clientId = clientId,
        text = text,
        theme = theme,
        shape = shape,
        size = size,
        logoAlignment = logoAlignment,
        fullWidth = fullWidth,
        onToken = onToken,
    )
) {
    GoogleSignInButton(it)
}

class GoogleSignInButton(ctx: Ctx<Props>) : Component<GoogleSignInButton.Props>(ctx) {

    // See: https://developers.google.com/identity/gsi/web/guides/display-button?hl=de

    //  PROPS  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class Props(
        val clientId: String,
        val onToken: (token: String) -> Unit,
        val text: String,
        val theme: String,
        val shape: String,
        val size: String,
        val logoAlignment: String,
        val fullWidth: Boolean,
    )

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private val cbName = "_google_sso_${uuid4().toString().replace("-", "")}"

    private var mounted by value(false)
    private var widthPx by value<Int?>(null)

    private fun handleGoogleSsoResponse(googleResponse: dynamic) {
        props.onToken(googleResponse.credential as String)
    }

    private fun recomputeWidth() {
        if (!props.fullWidth) return
        val w = dom?.offsetWidth ?: return
        widthPx = w
    }

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    init {
        lifecycle {
            onMount {
                window.asDynamic()[cbName] = ::handleGoogleSsoResponse

                mounted = true
                // compute initial width after mount
                recomputeWidth()

                // update on resize
                val listener: (dynamic) -> Unit = { _ -> recomputeWidth() }
                window.addEventListener("resize", listener)

                onUnmount {
                    window.removeEventListener("resize", listener)
                    window.asDynamic()[cbName] = undefined
                }
            }
        }
    }

    override fun VDom.render() {
        div {
            if (mounted) {
                script {
                    src = "https://accounts.google.com/gsi/client"
                    async = true
                }

                noui {
                    id = "g_id_onload"
                    data("client_id", props.clientId)
                    data("callback", cbName)
                    data("auto_prompt", "false")
                    data("auto_select", "false")
                }

                noui {
                    id = "g_id_signin"
                    classes = setOf("g_id_signin")
                    data("type", "standard")
                    data("theme", props.theme)
                    data("text", props.text)
                    data("shape", props.shape)
                    data("size", props.size)
                    data("logo_alignment", props.logoAlignment)

                    val w = if (props.fullWidth) {
                        (widthPx?.toString() ?: (dom?.offsetWidth?.toString() ?: ""))
                    } else {
                        ""
                    }

                    if (w.isNotBlank()) data("width", w)
                }
            }
        }
    }
}
