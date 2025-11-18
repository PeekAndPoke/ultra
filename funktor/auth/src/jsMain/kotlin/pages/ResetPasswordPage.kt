package de.peekandpoke.funktor.auth.pages

import de.peekandpoke.funktor.auth.AuthState
import de.peekandpoke.funktor.auth.model.AuthRecoverAccountRequest
import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.components.Ctx
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.routing.href
import de.peekandpoke.kraft.utils.dataLoader
import de.peekandpoke.kraft.vdom.VDom
import de.peekandpoke.ultra.semanticui.icon
import de.peekandpoke.ultra.semanticui.ui
import kotlinx.html.FlowContent
import kotlinx.html.Tag
import kotlinx.html.a
import kotlinx.html.div

@Suppress("FunctionName")
fun <USER> Tag.ResetPasswordPage(
    state: AuthState<USER>,
    provider: String,
    token: String,
) = comp(
    ResetPasswordPage.Props(
        state = state,
        provider = provider,
        token = token,
    )
) {
    ResetPasswordPage(it)
}

class ResetPasswordPage<USER>(ctx: Ctx<Props<USER>>) : Component<ResetPasswordPage.Props<USER>>(ctx) {

    //  PROPS  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class Props<USER>(
        val state: AuthState<USER>,
        val provider: String,
        val token: String,
    )

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    val authState get() = props.state

    val tokenLoader = dataLoader {
        authState.api.recoverAccountValidatePasswordResetToken(
            AuthRecoverAccountRequest.ValidatePasswordResetToken(
                provider = props.provider,
                token = props.token,
            )
        )
    }

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        AuthPageLayouts {
            renderFullscreenBackgroundLayout(authState.frontend.config) {
                ui.header { +"Reset Password" }

                tokenLoader(this) {
                    loading {
                        ui.basic.loading.segment {
                        }
                    }
                    error {
                        ui.basic.segment {
                            ui.error.message {
                                icon.exclamation()
                                +"Invalid token. Please try again."
                            }

                            renderBackLink()
                        }
                    }
                    loaded {
                        +"TOKEN OK!"
                    }
                }
            }
        }
    }

    private fun FlowContent.renderBackLink() {
        div {
            a {
                href(props.state.frontend.routes.login())

                icon.angle_left()
                +"Back"
            }
        }
    }
}
