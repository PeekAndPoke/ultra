package de.peekandpoke.funktor.auth.pages

import de.peekandpoke.funktor.auth.AuthState
import de.peekandpoke.funktor.auth.asFormRule
import de.peekandpoke.funktor.auth.model.AuthRealmModel
import de.peekandpoke.funktor.auth.model.AuthRecoverAccountRequest
import de.peekandpoke.funktor.auth.model.AuthRecoverAccountResponse
import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.components.Ctx
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.forms.formController
import de.peekandpoke.kraft.routing.href
import de.peekandpoke.kraft.semanticui.forms.UiPasswordField
import de.peekandpoke.kraft.utils.dataLoader
import de.peekandpoke.kraft.utils.doubleClickProtection
import de.peekandpoke.kraft.utils.launch
import de.peekandpoke.kraft.vdom.VDom
import de.peekandpoke.ultra.html.onClick
import de.peekandpoke.ultra.html.onSubmit
import de.peekandpoke.ultra.semanticui.icon
import de.peekandpoke.ultra.semanticui.ui
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.html.FlowContent
import kotlinx.html.Tag
import kotlinx.html.a
import kotlinx.html.div
import kotlinx.html.p

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

    private data class Data(
        val realm: AuthRealmModel,
        val response: AuthRecoverAccountResponse.ValidatePasswordResetToken,
    )

    private sealed interface DisplayState {
        data class Input(
            val password: String = "",
            val errorMessage: String? = null,
        ) : DisplayState

        data object Success : DisplayState
    }

    private val authState get() = props.state
    private var displayState: DisplayState by value(DisplayState.Input())

    private val loader = dataLoader {
        authState.api.getRealm()
            .map { it.data!! }
            .map { realm ->
                val response = authState.api.recoverAccountValidatePasswordResetToken(
                    AuthRecoverAccountRequest.ValidatePasswordResetToken(
                        provider = props.provider,
                        token = props.token,
                    )
                ).map { it.data!! }.first()

                Data(realm = realm, response = response)
            }
    }

    private val formCtrl = formController()
    private val noDblClick = doubleClickProtection()

    private suspend fun resetPassword(state: DisplayState.Input) = noDblClick.runBlocking {
        val result = authState.api.recoverAccountSetPasswordWithToken(
            AuthRecoverAccountRequest.SetPasswordWithToken(
                provider = props.provider,
                token = props.token,
                password = state.password,
            )
        ).map { it.data!! }
            .catch { }
            .firstOrNull()

        displayState = when (result?.success) {
            true -> DisplayState.Success

            else -> state.copy(
                errorMessage = "Password could not be reset. Please try again.",
            )
        }
    }

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        AuthPageLayouts {
            renderFullscreenBackgroundLayout(authState.frontend.config) {
                ui.header { +"Reset Password" }

                loader(this) {
                    loading {
                        ui.basic.loading.segment {
                        }
                    }
                    error {
                        ui.red.message {
                            icon.exclamation()
                            +"Error loading. Please try again."

                            ui.basic.fluid.button {
                                onClick {
                                    loader.reload()
                                }
                                +"Retry"
                            }
                        }
                    }
                    loaded { data ->
                        if (data.response.success.not()) {
                            renderInvalidToken()
                        } else {
                            renderContent(data)
                        }
                    }
                }
            }
        }
    }

    private fun FlowContent.renderContent(data: Data) {
        console.log("state", displayState)

        when (val s = displayState) {
            is DisplayState.Input -> renderInput(data, s)
            is DisplayState.Success -> renderSuccess()
        }
    }

    private fun FlowContent.renderInput(data: Data, s: DisplayState.Input) {
        ui.form Form {
            onSubmit { evt ->
                evt.preventDefault()
            }

            p {
                +"Please enter your new password"
            }

            UiPasswordField(s.password, { displayState = s.copy(password = it) }) {
                placeholder("New Password")
                revealPasswordIcon()
                accepts(
                    data.realm.passwordPolicy.asFormRule()
                )
            }

            ui.field {
                ui.primary.fluid
                    .givenNot(noDblClick.canRun) { loading }
                    .givenNot(formCtrl.isValid) { disabled }
                    .button Submit {
                    onClick {
                        if (formCtrl.validate()) {
                            launch {
                                resetPassword(s)
                            }
                        }
                    }
                    +"Reset Password"
                }
            }

            s.errorMessage?.let {
                ui.red.message {
                    icon.exclamation()
                    +it
                }
            }

            ui.hidden.divider()

            renderBackLink()
        }
    }

    private fun FlowContent.renderSuccess() {
        ui.success.message {
            icon.check()
            +"Password reset successfully."
        }

        renderBackLink()
    }

    private fun FlowContent.renderInvalidToken() {
        ui.error.message {
            icon.exclamation()
            +"The recovery token is invalid or has expired."
        }

        renderBackLink()
    }

    private fun FlowContent.renderBackLink() {
        div {
            a {
                href(props.state.frontend.routes.login())

                icon.angle_left()
                +"Back to Login"
            }
        }
    }
}
