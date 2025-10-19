package de.peekandpoke.funktor.auth.widgets

import de.peekandpoke.funktor.auth.AuthState
import de.peekandpoke.funktor.auth.asFormRule
import de.peekandpoke.funktor.auth.model.AuthProviderModel
import de.peekandpoke.funktor.auth.model.AuthUpdateRequest
import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.components.Ctx
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.forms.formController
import de.peekandpoke.kraft.semanticui.forms.UiPasswordField
import de.peekandpoke.kraft.utils.doubleClickProtection
import de.peekandpoke.kraft.utils.launch
import de.peekandpoke.kraft.vdom.VDom
import de.peekandpoke.ultra.html.onSubmit
import de.peekandpoke.ultra.semanticui.ui
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
        val result = auth.requestAuthUpdate(
            AuthUpdateRequest.SetPassword(
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
