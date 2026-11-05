package io.peekandpoke.funktor.auth.pages

import io.peekandpoke.funktor.auth.AuthState
import io.peekandpoke.funktor.auth.asFormRule
import io.peekandpoke.funktor.auth.model.AuthActivateAccountRequest
import io.peekandpoke.funktor.auth.model.AuthActivateAccountResponse
import io.peekandpoke.funktor.auth.model.AuthCancelSignUpRequest
import io.peekandpoke.funktor.auth.model.AuthDisplayName
import io.peekandpoke.funktor.auth.model.AuthRealmModel
import io.peekandpoke.funktor.auth.model.AuthSignInResponse
import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.forms.formController
import io.peekandpoke.kraft.routing.href
import io.peekandpoke.kraft.semanticui.forms.UiInputField
import io.peekandpoke.kraft.semanticui.forms.UiPasswordField
import io.peekandpoke.kraft.utils.dataLoader
import io.peekandpoke.kraft.utils.doubleClickProtection
import io.peekandpoke.kraft.utils.launch
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.html.onClick
import io.peekandpoke.ultra.html.onSubmit
import io.peekandpoke.ultra.semanticui.icon
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.coroutines.flow.map
import kotlinx.html.FlowContent
import kotlinx.html.Tag
import kotlinx.html.a
import kotlinx.html.div
import kotlinx.html.p

/**
 * Serves the activation mail's two deep-links.
 *
 * **Opening the page changes nothing** (it only reads the realm, for its password policy). Mail gateways
 * (Safe Links, Mimecast, Proofpoint) open links in a sandbox, so a link that activated on load would activate
 * an account nobody chose to. Only a click POSTs: `Mode.Activate` asks for the account's first password and the
 * display name — setting them activates the account and signs the user in, or hands a choice of several
 * organisations to the login page's picker — next to "I did not sign up — cancel";
 * `Mode.Cancel` (the mail's cancel link, whose token cannot activate) offers only "Cancel this sign-up" and
 * points at the activation link for the other way.
 */
@Suppress("FunctionName")
fun <USER> Tag.ActivateAccountPage(
    state: AuthState<USER>,
    provider: String,
    token: String,
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
        val token: String,
        val mode: Mode,
    )

    /** Which of the mail's two links brought the user here. */
    enum class Mode { Activate, Cancel }

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private sealed interface DisplayState {
        /** A link from the mail, nothing done yet — or the server refused the input: the user chooses. */
        data class Choose(
            val password: String = "",
            val displayName: String = "",
            /** Why the server refused the last submit; the link is still usable. */
            val refused: AuthActivateAccountResponse.Failure? = null,
        ) : DisplayState

        /** The account is activated, but the realm handed out no session (e.g. no organisation yet). */
        data object Activated : DisplayState

        /** The click cancelled the sign-up; the address is free again. */
        data object Cancelled : DisplayState

        /** The server answered "no" — the link expired, was already used, or was superseded by a newer mail. */
        data object LinkNotUsable : DisplayState

        /** The server could not be asked, so nothing is known. [retry] repeats the same click. */
        data class Failed(val retry: suspend () -> Unit) : DisplayState
    }

    private val authState get() = props.state

    private var displayState: DisplayState by value(DisplayState.Choose())

    private val realm = dataLoader {
        authState.api.getRealm().map { it.data!! }
    }

    private val formCtrl = formController()
    private val noDblClick = doubleClickProtection()

    /** Edits the CURRENT choice, not the rendered one: two fields changed before a re-render must not undo each other. */
    private fun editChoice(edit: (DisplayState.Choose) -> DisplayState.Choose) {
        (displayState as? DisplayState.Choose)?.let { displayState = edit(it) }
    }

    private suspend fun activate(input: DisplayState.Choose) {
        noDblClick.runBlocking { activateNow(input) }
    }

    private suspend fun activateNow(input: DisplayState.Choose) {
        val result = authState.activateAccount(
            AuthActivateAccountRequest(
                provider = props.provider,
                token = props.token,
                password = input.password,
                displayName = input.displayName,
            )
        )

        displayState = when {
            result == null -> DisplayState.Failed(retry = { activate(input) })
            result.success -> DisplayState.Activated
            // The input, not the link: keep the form, say why.
            result.failure != null -> input.copy(refused = result.failure)
            else -> DisplayState.LinkNotUsable
        }

        // Only THIS answer's session: a browser already signed in as someone else must still see the outcome.
        when (result?.signIn) {
            is AuthSignInResponse.Success -> {
                val uri = authState.router().strategy.render(authState.frontend.config.redirectAfterLogin)
                authState.redirectAfterLogin(uri)
            }

            // Several organisations: `AuthState` holds the selection, the login page shows the picker.
            is AuthSignInResponse.OrgSelectionRequired ->
                authState.router().navToUri(authState.router().strategy.render(authState.frontend.routes.login()))

            null -> Unit
        }
    }

    private suspend fun cancel() {
        noDblClick.runBlocking { cancelNow() }
    }

    private suspend fun cancelNow() {
        val result = authState.cancelSignUp(
            AuthCancelSignUpRequest(provider = props.provider, token = props.token)
        )

        displayState = when (result?.success) {
            true -> DisplayState.Cancelled
            false -> DisplayState.LinkNotUsable
            null -> DisplayState.Failed(retry = { cancel() })
        }
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
                }
            }
        }
    }

    private fun FlowContent.renderChoose(s: DisplayState.Choose) {
        when (props.mode) {
            Mode.Activate -> {
                realm(this) {
                    loading {
                        ui.basic.loading.segment {}
                    }
                    error {
                        ui.red.message {
                            icon.exclamation()
                            +"We could not reach the server. Nothing has changed - please try again."
                        }

                        ui.basic.fluid.button {
                            onClick { realm.reload() }
                            +"Retry"
                        }
                    }
                    loaded { data -> renderChoosePassword(s, data) }
                }

                ui.hidden.divider()

                p {
                    +"If you did not create this account, someone may have used your address. "
                    +"Cancel it, and nobody can use it."
                }

                ui.field {
                    ui.basic.fluid.givenNot(noDblClick.canRun) { loading }.button {
                        onClick { launch { cancel() } }
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
                        onClick { launch { cancel() } }
                        +"Cancel this sign-up"
                    }
                }

                ui.hidden.divider()

                // No "activate instead" here: this link carries the CANCEL token, which cannot activate.
                p {
                    +"Did you sign up after all? Then use the \"Choose your password\" link in the same mail instead."
                }
            }
        }

        ui.hidden.divider()

        renderBackLink()
    }

    private fun FlowContent.renderChoosePassword(s: DisplayState.Choose, realm: AuthRealmModel) {
        ui.form Form {
            onSubmit { evt -> evt.preventDefault() }

            p { +"Choose your name and a password for your account. Setting them activates the account." }

            when (s.refused) {
                AuthActivateAccountResponse.Failure.WeakPassword -> ui.error.visible.message {
                    +"The server did not accept this password. "
                    +realm.passwordPolicy.description
                }

                AuthActivateAccountResponse.Failure.InvalidDisplayName -> ui.error.visible.message {
                    +"The server did not accept this name. "
                    +AuthDisplayName.DESCRIPTION
                }

                null -> Unit
            }

            UiInputField(s.displayName, { name -> editChoice { it.copy(displayName = name) } }) {
                placeholder("Your name")
                accepts(AuthDisplayName.asFormRule())
            }

            UiPasswordField(s.password, { password -> editChoice { it.copy(password = password) } }) {
                placeholder("Password")
                revealPasswordIcon()
                accepts(realm.passwordPolicy.asFormRule())
            }

            ui.field {
                ui.primary.fluid
                    .givenNot(noDblClick.canRun) { loading }
                    .givenNot(formCtrl.isValid) { disabled }
                    .button Submit {
                    onClick { formCtrl.validate { activate(s.copy(refused = null)) } }
                    +"Activate my account"
                }
            }
        }
    }

    private fun FlowContent.renderActivated() {
        ui.success.message {
            icon.check()
            +"Your account is active."
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
            +"It may have expired, it may already have been used, or a newer mail replaced it. If your account is "
            +"active, just sign in. If it is not, sign up again with the same address: we will send a new link."
        }

        renderBackLink()
    }

    private fun FlowContent.renderFailed(s: DisplayState.Failed) {
        ui.red.message {
            icon.exclamation()
            +"We could not reach the server - please try again. If your account was activated meanwhile, sign in "
            +"with the password you chose."
        }

        ui.field {
            ui.basic.fluid.givenNot(noDblClick.canRun) { loading }.button {
                onClick { launch { s.retry() } }
                +"Retry"
            }
        }

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
