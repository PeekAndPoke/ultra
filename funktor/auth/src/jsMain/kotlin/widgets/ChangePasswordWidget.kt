package io.peekandpoke.funktor.auth.widgets

import io.peekandpoke.funktor.auth.AuthState
import io.peekandpoke.funktor.auth.asFormRule
import io.peekandpoke.funktor.auth.model.AuthProviderModel
import io.peekandpoke.funktor.auth.model.AuthSetPasswordRequest
import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.forms.formController
import io.peekandpoke.kraft.semanticui.forms.UiPasswordField
import io.peekandpoke.kraft.utils.doubleClickProtection
import io.peekandpoke.kraft.utils.launch
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.html.onSubmit
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.html.FlowContent
import kotlinx.html.Tag

@Suppress("FunctionName")
fun <USER> Tag.ChangePasswordWidget(
    state: AuthState<USER>,
    onUpdate: (success: Boolean) -> Unit = {},
) = comp(
    ChangePasswordWidget.Props(
        state = state,
        onUpdate = onUpdate,
    )
) {
    ChangePasswordWidget(it)
}

class ChangePasswordWidget<USER>(ctx: Ctx<Props<USER>>) : Component<ChangePasswordWidget.Props<USER>>(ctx) {

    //  PROPS  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class Props<USER>(
        val state: AuthState<USER>,
        val onUpdate: (success: Boolean) -> Unit,
    )

    private sealed interface State {
        data object Error : State
        data object Edit : State
        data object Done : State
    }

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private val auth get() = props.state
    private val policy get() = auth.getPasswordPolicy()

    private val provider get() = auth().realm?.providers?.first { it.type == AuthProviderModel.TYPE_EMAIL_PASSWORD }
    private val userId get() = auth().tokenUserId

    private var newPassword by value("")

    private var state: State by value(
        when (provider?.type) {
            AuthProviderModel.TYPE_EMAIL_PASSWORD -> State.Edit
            else -> State.Error
        }
    )

    private val formCtrl = formController()
    private val noDblClick = doubleClickProtection()

    private suspend fun updatePassword() = noDblClick.runBlocking {
        val result = auth.requestSetPassword(
            AuthSetPasswordRequest(
                provider = provider?.id ?: "",
                userId = userId ?: "",
                newPassword = newPassword,
            )
        )

        if (result) {
            state = State.Done
        }

        props.onUpdate(result)
    }

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        when (state) {
            is State.Error -> renderError()
            is State.Edit -> renderEdit()
            is State.Done -> renderDone()
        }
    }

    private fun FlowContent.renderError() {
        ui.error.message {
            +"Password update not possible."
        }
    }

    private fun FlowContent.renderDone() {
        ui.success.message {
            +"Password updated successfully."
        }
    }

    private fun FlowContent.renderEdit() {
        ui.form Form {
            onSubmit { evt ->
                evt.preventDefault()

                if (formCtrl.validate()) {
                    launch {
                        updatePassword()
                    }
                }
            }

            UiPasswordField(::newPassword) {
                label("New Password")
                revealPasswordIcon()

                accepts(
                    policy.asFormRule(),
                )
            }

            ui.basic.primary.fluid
                .givenNot(noDblClick.canRun) { loading }
                .givenNot(formCtrl.isValid) { disabled }
                .button Submit {
                +"Update Password"
            }
        }
    }
}
