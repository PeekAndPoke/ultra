package io.peekandpoke.funktor.auth.pages

import io.peekandpoke.funktor.auth.AuthState
import io.peekandpoke.funktor.auth.model.AuthActivateAccountRequest
import io.peekandpoke.funktor.auth.model.AuthCancelSignUpRequest
import io.peekandpoke.funktor.auth.model.AuthResendActivationRequest
import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.routing.href
import io.peekandpoke.kraft.utils.doubleClickProtection
import io.peekandpoke.kraft.utils.launch
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.html.onClick
import io.peekandpoke.ultra.semanticui.icon
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.html.FlowContent
import kotlinx.html.Tag
import kotlinx.html.a
import kotlinx.html.div
import kotlinx.html.p
import kotlin.math.ceil
import kotlin.time.Duration

/**
 * Serves the activation mail's two deep-links, and doubles as the "I need a new link" page.
 *
 * **Opening the page changes nothing.** Mail gateways (Safe Links, Mimecast, Proofpoint) open links
 * in a sandbox, so a link that activated on load would activate an account nobody chose to — including
 * one an attacker pre-registered with a victim's address. Only a click POSTs: `Mode.Activate` offers
 * "Activate" and "I did not sign up — cancel"; `Mode.Cancel` (the mail's cancel link, whose token cannot
 * activate) offers only "Cancel this sign-up" and points at the activation link for the other way.
 *
 * [token] is null when the LOGIN page sent the user here after a sign-in was refused for a
 * not-yet-activated account — there is nothing to redeem then, only a resend to offer.
 */
@Suppress("FunctionName")
fun <USER> Tag.ActivateAccountPage(
    state: AuthState<USER>,
    provider: String,
    token: String?,
    mode: ActivateAccountPage.Mode = ActivateAccountPage.Mode.Activate,
) = comp(
    ActivateAccountPage.Props(
        state = state,
        provider = provider,
        token = token,
        mode = mode,
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
        val mode: Mode,
    )

    /** Which of the mail's two links brought the user here. */
    enum class Mode { Activate, Cancel }

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private sealed interface DisplayState {
        /** A link from the mail, nothing done yet: the user chooses. */
        data class Choose(val token: String) : DisplayState

        /** The click activated the account. */
        data object Activated : DisplayState

        /** The click cancelled the sign-up; the address is free again. */
        data object Cancelled : DisplayState

        /**
         * The server answered "no" — the link expired, was already used, or was superseded by a newer
         * mail. There is no resend from here: resend is authorized by a token that only a completed
         * password check produces, so the way back is the login page.
         */
        data object LinkNotUsable : DisplayState

        /** The server could not be asked, so nothing is known. [retry] repeats the same click. */
        data class Failed(val retry: suspend () -> Unit) : DisplayState

        /** Arrived from the login page, which proved the password and left a resend token. */
        data class Offer(
            val pending: AuthState.PendingActivation,
            val outcome: Outcome? = null,
        ) : DisplayState {
            sealed interface Outcome {
                data object Sent : Outcome

                /** The account's growing resend delay has not elapsed yet. */
                data class TooSoon(val nextResendAt: MpInstant) : Outcome

                /** Refused for another reason (used up, activated, expired) or the mail failed. */
                data object NotSent : Outcome

                /** The server could not be asked. */
                data object Failed : Outcome
            }
        }
    }

    private val authState get() = props.state

    private var displayState: DisplayState by value(
        when (val token = props.token) {
            null ->
                // No token means the login page sent us here and left the resend authorization.
                //
                // Read AND CLEARED: the carrier holds the address the user just typed, and leaving it
                // behind means the next person to open this page in a shared browser sees it prefilled.
                authState.pendingActivation
                    ?.also { authState.clearPendingActivation() }
                    ?.let { DisplayState.Offer(pending = it) }
                    ?: DisplayState.LinkNotUsable

            else -> DisplayState.Choose(token = token)
        }
    )

    private val noDblClick = doubleClickProtection()

    private suspend fun activate(token: String) {
        noDblClick.runBlocking { activateNow(token) }
    }

    private suspend fun activateNow(token: String) {
        val result = authState.activateAccount(
            AuthActivateAccountRequest(provider = props.provider, token = token)
        )

        displayState = when (result?.success) {
            true -> DisplayState.Activated
            false -> DisplayState.LinkNotUsable
            null -> DisplayState.Failed(retry = { activate(token) })
        }
    }

    private suspend fun cancel(token: String) {
        noDblClick.runBlocking { cancelNow(token) }
    }

    private suspend fun cancelNow(token: String) {
        val result = authState.cancelSignUp(
            AuthCancelSignUpRequest(provider = props.provider, token = token)
        )

        displayState = when (result?.success) {
            true -> DisplayState.Cancelled
            false -> DisplayState.LinkNotUsable
            null -> DisplayState.Failed(retry = { cancel(token) })
        }
    }

    private suspend fun resend(s: DisplayState.Offer) = noDblClick.runBlocking {
        val result = authState.resendActivation(
            AuthResendActivationRequest(provider = props.provider, token = s.pending.resendToken)
        )

        // Reporting the real outcome is safe here precisely because this call was authorized: the
        // caller already proved the password, so the answer tells them nothing they did not know.
        val nextResendAt = result?.nextResendAt

        displayState = s.copy(
            outcome = when {
                result == null -> DisplayState.Offer.Outcome.Failed
                result.sent -> DisplayState.Offer.Outcome.Sent
                nextResendAt != null -> DisplayState.Offer.Outcome.TooSoon(nextResendAt)
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

                when (val s = displayState) {
                    is DisplayState.Choose -> renderChoose(s)
                    is DisplayState.Activated -> renderActivated()
                    is DisplayState.Cancelled -> renderCancelled()
                    is DisplayState.LinkNotUsable -> renderLinkNotUsable()
                    is DisplayState.Failed -> renderFailed(s)
                    is DisplayState.Offer -> renderOffer(s)
                }
            }
        }
    }

    private fun FlowContent.renderChoose(s: DisplayState.Choose) {
        when (props.mode) {
            Mode.Activate -> {
                p { +"Please confirm that you created this account." }

                ui.field {
                    ui.primary.fluid.givenNot(noDblClick.canRun) { loading }.button {
                        onClick { launch { activate(s.token) } }
                        +"Activate my account"
                    }
                }

                ui.hidden.divider()

                p {
                    +"If you did not create this account, someone may have used your address. "
                    +"Cancel it, and nobody can use it."
                }

                ui.field {
                    ui.basic.fluid.givenNot(noDblClick.canRun) { loading }.button {
                        onClick { launch { cancel(s.token) } }
                        +"I did not sign up - cancel this account"
                    }
                }
            }

            Mode.Cancel -> {
                p {
                    +"If you did not create this account, someone may have used your address. "
                    +"Cancel it, and nobody can use it - the address becomes free again."
                }

                ui.field {
                    ui.red.fluid.givenNot(noDblClick.canRun) { loading }.button {
                        onClick { launch { cancel(s.token) } }
                        +"Cancel this sign-up"
                    }
                }

                ui.hidden.divider()

                // No "activate instead" here: this link carries the CANCEL token, which cannot activate.
                p {
                    +"Did you sign up after all? Then use the \"Activate account\" link in the same mail instead."
                }
            }
        }

        ui.hidden.divider()

        renderBackLink()
    }

    private fun FlowContent.renderActivated() {
        ui.success.message {
            icon.check()
            +"Your account is activated. You can sign in now."
        }

        renderBackLink()
    }

    private fun FlowContent.renderCancelled() {
        ui.success.message {
            icon.check()
            +"The sign-up is cancelled. Nobody can use this account, and the address is free again."
        }

        renderBackLink()
    }

    private fun FlowContent.renderLinkNotUsable() {
        ui.error.message {
            icon.exclamation()
            +"This link is not valid any more."
        }

        // The cancel link is the way out for someone who did NOT sign up; "try signing in" would be advice
        // for the wrong person. It lives as long as the sign-up, so a dead one means the sign-up is over.
        if (props.mode == Mode.Cancel) {
            p {
                +"The sign-up was already cancelled, it has expired - then the address is free again and "
                +"nothing more needs doing - or it was activated. If it was activated and you did not do that, "
                +"use \"Forgot password\" on the login page: the reset link goes to your mailbox only."
            }

            renderBackLink()
            return
        }

        p {
            // ALL possibilities, because the page cannot tell them apart and the most common one is the
            // happy case: a second click on a link that already worked.
            +"It may have expired, it may already have been used, or a newer mail replaced it. Try signing in - "
            +"if your account is active you are done, and if it is not we will offer you a new link."
        }

        renderBackLink()
    }

    private fun FlowContent.renderFailed(s: DisplayState.Failed) {
        ui.red.message {
            icon.exclamation()
            +"We could not reach the server. Nothing has changed - please try again."
        }

        ui.field {
            ui.basic.fluid.givenNot(noDblClick.canRun) { loading }.button {
                onClick { launch { s.retry() } }
                +"Retry"
            }
        }

        renderBackLink()
    }

    private fun FlowContent.renderOffer(s: DisplayState.Offer) {
        when (val outcome = s.outcome) {
            null -> {
                // Lead with what actually happened, and with the mail that is ALREADY in their inbox.
                // Someone who signed up a minute ago and tried to log in does not need a new link, they
                // need to be told to go and read the first one.
                ui.info.message {
                    icon.mail()
                    +"Your account is not activated yet. We sent a link to ${s.pending.email} - "
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

            // Only said when the server says so: a "recently sent" claim for any other refusal would
            // leave the user waiting for a mail that is not coming.
            is DisplayState.Offer.Outcome.TooSoon -> ui.warning.message {
                icon.clock()
                +"We already sent a link to ${s.pending.email} recently. "
                +"Please check your inbox, or sign in again ${untilText(outcome.nextResendAt)} to request another."
            }

            DisplayState.Offer.Outcome.NotSent -> ui.warning.message {
                icon.exclamation()
                +"We could not send a new link. If your account is already active, just sign in. "
                +"If you signed up a while ago, the sign-up may have expired - then simply sign up again."
            }

            DisplayState.Offer.Outcome.Failed -> ui.red.message {
                icon.exclamation()
                +"We could not reach the server. Please sign in again to retry."
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

    /** "in about 12 minutes" / "in about 3 hours", rounded UP so the user never comes back too early. */
    private fun untilText(at: MpInstant): String {
        val left: Duration = at - MpInstant.now()

        return when {
            left.inWholeMinutes < 1 -> "in a minute"
            left.inWholeMinutes < 90 -> "in about ${ceil(left.inWholeSeconds / 60.0).toLong()} minutes"
            else -> "in about ${ceil(left.inWholeMinutes / 60.0).toLong()} hours"
        }
    }
}
