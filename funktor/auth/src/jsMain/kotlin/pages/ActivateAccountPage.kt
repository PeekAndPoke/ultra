package io.peekandpoke.funktor.auth.pages

import io.peekandpoke.funktor.auth.AuthState
import io.peekandpoke.funktor.auth.model.AuthActivateAccountRequest
import io.peekandpoke.funktor.auth.model.AuthResendActivationRequest
import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.routing.href
import io.peekandpoke.kraft.utils.dataLoader
import io.peekandpoke.kraft.utils.doubleClickProtection
import io.peekandpoke.kraft.utils.launch
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.html.onClick
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

        /**
         * The link did not work — expired, already used, or the account was activated earlier and this
         * is a reload of a consumed link.
         *
         * There is no resend from here: resend is authorized by a token that only a completed password
         * check produces, so the way back is the login page. That is not a dead end — signing in either
         * works (the account was already activated) or hands out a fresh resend offer.
         */
        data object LinkNotUsable : DisplayState

        /** Arrived from the login page, which proved the password and left a resend token. */
        data class Offer(
            val pending: AuthState.PendingActivation,
            val outcome: Outcome? = null,
        ) : DisplayState {
            enum class Outcome { Sent, NotSent }
        }
    }

    private val authState get() = props.state

    private var displayState: DisplayState by value(
        // No token to redeem means the login page sent us here; it also left the resend authorization.
        // Without one there is nothing this page can do but point back at the login form.
        //
        // Read AND CLEARED: the carrier holds the address the user just typed, and leaving it behind
        // means the next person to open this page in a shared browser sees it prefilled. This page has
        // taken what it needs into its own state, so nothing is lost.
        authState.pendingActivation
            ?.also { authState.clearPendingActivation() }
            ?.let { DisplayState.Offer(pending = it) }
            ?: DisplayState.LinkNotUsable
    )

    /**
     * Redeems the token, if there is one.
     *
     * A `dataLoader` rather than an event handler because the deep-link IS the action: there is
     * nothing for the user to click when they arrive from the mail.
     */
    private val loader = dataLoader {
        val token = props.token
            ?: return@dataLoader flowOf(false)

        val activated = authState.activateAccount(
            AuthActivateAccountRequest(provider = props.provider, token = token)
        )?.success == true

        displayState = when {
            activated -> DisplayState.Activated
            else -> DisplayState.LinkNotUsable
        }

        flowOf(activated)
    }

    private val noDblClick = doubleClickProtection()

    private suspend fun resend(s: DisplayState.Offer) = noDblClick.runBlocking {
        val result = authState.resendActivation(
            AuthResendActivationRequest(provider = props.provider, token = s.pending.resendToken)
        )

        // Reporting the real outcome is safe here precisely because this call was authorized: the
        // caller already proved the password, so `sent` tells them nothing they did not already know.
        displayState = s.copy(
            outcome = when (result?.sent) {
                true -> DisplayState.Offer.Outcome.Sent
                else -> DisplayState.Offer.Outcome.NotSent
            }
        )
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
                            is DisplayState.LinkNotUsable -> renderLinkNotUsable()
                            is DisplayState.Offer -> renderOffer(s)
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

    private fun FlowContent.renderLinkNotUsable() {
        ui.error.message {
            icon.exclamation()
            +"This activation link is not valid any more."
        }

        p {
            // BOTH possibilities, because the page cannot tell them apart and the far more common one
            // is the happy case: a reload of a link that already worked. Telling only the failure story
            // sends an activated user hunting for a new mail that will never come.
            +"It may have expired, or it may already have been used. Try signing in — "
            +"if your account is active you are done, and if it is not we will offer you a new link."
        }

        renderBackLink()
    }

    private fun FlowContent.renderOffer(s: DisplayState.Offer) {
        when (s.outcome) {
            null -> {
                // Lead with what actually happened, and with the mail that is ALREADY in their inbox.
                // Someone who signed up a minute ago and tried to log in does not need a new link, they
                // need to be told to go and read the first one.
                ui.info.message {
                    icon.mail()
                    +"Your account is not activated yet. We sent a link to ${s.pending.email} — "
                    +"please check your inbox and your spam folder."
                }

                ui.field {
                    ui.primary.fluid
                        .givenNot(noDblClick.canRun) { loading }
                        .button {
                        onClick { launch { resend(s) } }
                        +"Send a new link"
                    }
                }
            }

            DisplayState.Offer.Outcome.Sent -> ui.success.message {
                icon.check()
                +"A new link is on its way to ${s.pending.email}."
            }

            // The honest answer, which the neutral-response version of this endpoint could not give:
            // the cooldown suppressed it, and telling the user a mail is coming would be a lie that
            // leaves them waiting for it.
            DisplayState.Offer.Outcome.NotSent -> ui.warning.message {
                icon.clock()
                +"We already sent a link to ${s.pending.email} recently. "
                +"Please check your inbox, or sign in again in a few minutes to request another."
            }
        }

        ui.hidden.divider()

        renderBackLink()
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
