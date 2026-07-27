package io.peekandpoke.funktor.auth.pages

import io.peekandpoke.funktor.auth.AuthState
import io.peekandpoke.funktor.auth.model.AuthActivateAccountRequest
import io.peekandpoke.funktor.auth.model.AuthResendActivationRequest
import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.forms.formController
import io.peekandpoke.kraft.routing.href
import io.peekandpoke.kraft.semanticui.forms.UiInputField
import io.peekandpoke.kraft.utils.dataLoader
import io.peekandpoke.kraft.utils.doubleClickProtection
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.html.onClick
import io.peekandpoke.ultra.html.onSubmit
import io.peekandpoke.ultra.semanticui.icon
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.coroutines.flow.flowOf
import kotlinx.html.FlowContent
import kotlinx.html.Tag
import kotlinx.html.a
import kotlinx.html.div
import kotlinx.html.p

/**
 * Serves the activation deep-link from the sign-up mail, and doubles as the "I need a new link" page.
 *
 * [token] is null when the LOGIN page sent the user here after a sign-in was refused for a
 * not-yet-activated account — there is no token to redeem in that case, only a resend to offer.
 */
@Suppress("FunctionName")
fun <USER> Tag.ActivateAccountPage(
    state: AuthState<USER>,
    provider: String,
    token: String?,
) = comp(
    ActivateAccountPage.Props(
        state = state,
        provider = provider,
        token = token,
    )
) {
    ActivateAccountPage(it)
}

class ActivateAccountPage<USER>(ctx: Ctx<Props<USER>>) : Component<ActivateAccountPage.Props<USER>>(ctx) {

    //  PROPS  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class Props<USER>(
        val state: AuthState<USER>,
        val provider: String,
        val token: String?,
    )

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private sealed interface DisplayState {
        /** The token was redeemed. */
        data object Activated : DisplayState

        /** Asking for a new link — either the token failed, or we arrived without one. */
        data class Resend(
            val email: String,
            val tokenWasInvalid: Boolean,
            val sent: Boolean = false,
        ) : DisplayState
    }

    private val authState get() = props.state

    private var displayState: DisplayState by value(
        DisplayState.Resend(
            // Prefilled from the login attempt when the login page sent us here, and empty when the
            // user opened the page cold.
            email = authState.pendingActivation?.email.orEmpty(),
            tokenWasInvalid = false,
        )
    )

    /**
     * Redeems the token, if there is one.
     *
     * A `dataLoader` rather than an event handler because the deep-link IS the action: there is
     * nothing for the user to click when they arrive from the mail.
     */
    private val loader = dataLoader {
        val token = props.token

        // Nothing to redeem — the page is in resend mode and the initial display state already says
        // so.
        if (token == null) return@dataLoader flowOf(false)

        val result = authState.activateAccount(
            AuthActivateAccountRequest(provider = props.provider, token = token)
        )

        val activated = result?.success == true

        displayState = when {
            activated -> DisplayState.Activated

            // An expired or already-used link. Offer a new one rather than a dead end — this is the
            // whole reason the resend form lives on this page.
            else -> DisplayState.Resend(
                email = authState.pendingActivation?.email.orEmpty(),
                tokenWasInvalid = true,
            )
        }

        flowOf(activated)
    }

    private val formCtrl = formController()
    private val noDblClick = doubleClickProtection()

    private suspend fun resend(s: DisplayState.Resend) = noDblClick.runBlocking {
        authState.resendActivation(
            AuthResendActivationRequest(provider = props.provider, email = s.email)
        )

        // ALWAYS the same confirmation. The server answers identically for an unknown address, an
        // already-activated account and a request inside the cooldown window, and the UI must not
        // undo that by reporting anything more specific.
        displayState = s.copy(sent = true)
    }

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        AuthPageLayouts {
            renderFullscreenBackgroundLayout(authState.frontend.config) {
                renderBranding(authState.frontend.config)

                ui.header { +"Activate Account" }

                loader(this) {
                    loading {
                        ui.basic.loading.segment { }
                    }
                    error {
                        ui.red.message {
                            icon.exclamation()
                            +"Error activating your account. Please try again."

                            ui.basic.fluid.button {
                                onClick { loader.reload() }
                                +"Retry"
                            }
                        }

                        renderBackLink()
                    }
                    loaded {
                        when (val s = displayState) {
                            is DisplayState.Activated -> renderActivated()
                            is DisplayState.Resend -> renderResend(s)
                        }
                    }
                }
            }
        }
    }

    private fun FlowContent.renderActivated() {
        ui.success.message {
            icon.check()
            +"Your account is activated. You can sign in now."
        }

        renderBackLink()
    }

    private fun FlowContent.renderResend(s: DisplayState.Resend) {
        if (s.tokenWasInvalid) {
            ui.error.message {
                icon.exclamation()
                +"This activation link is invalid or has expired."
            }
        }

        if (s.sent) {
            ui.info.message {
                icon.mail()
                +"If the address belongs to an account that still needs activating, "
                +"a new link is on its way. Please check your inbox."
            }

            renderBackLink()

            return
        }

        ui.form Form {
            onSubmit { evt -> evt.preventDefault() }

            p {
                +"Enter your email address and we will send you a new activation link."
            }

            UiInputField(s.email, { displayState = s.copy(email = it) }) {
                placeholder("Email")
            }

            ui.field {
                ui.primary.fluid
                    .givenNot(noDblClick.canRun) { loading }
                    .givenNot(formCtrl.isValid) { disabled }
                    .button Submit {
                    onClick {
                        formCtrl.validate {
                            resend(s)
                        }
                    }
                    +"Send new link"
                }
            }

            ui.hidden.divider()

            renderBackLink()
        }
    }

    private fun FlowContent.renderBackLink() {
        div {
            a {
                href(authState.frontend.routes.login())

                icon.angle_left()
                +"Back to Login"
            }
        }
    }
}
