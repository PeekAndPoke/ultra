package io.peekandpoke.funktor.demo.adminapp.pages.funktorconf

import io.peekandpoke.funktor.demo.adminapp.Apis
import io.peekandpoke.funktor.demo.adminapp.Nav
import io.peekandpoke.funktor.demo.common.funktorconf.SaveAttendeeRequest
import io.peekandpoke.funktor.demo.common.funktorconf.TicketType
import io.peekandpoke.funktor.inspect.renderDefault
import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.forms.formController
import io.peekandpoke.kraft.forms.validation.strings.notBlank
import io.peekandpoke.kraft.routing.Router.Companion.router
import io.peekandpoke.kraft.semanticui.forms.UiInputField
import io.peekandpoke.kraft.semanticui.forms.old.select.SelectField
import io.peekandpoke.kraft.toasts.ToastsManager.Companion.toasts
import io.peekandpoke.kraft.utils.dataLoader
import io.peekandpoke.kraft.utils.doubleClickProtection
import io.peekandpoke.kraft.utils.launch
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.html.onChange
import io.peekandpoke.ultra.html.onClick
import io.peekandpoke.ultra.semanticui.icon
import io.peekandpoke.ultra.semanticui.noui
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.html.FlowContent
import kotlinx.html.Tag
import kotlinx.html.checkBoxInput
import org.w3c.dom.HTMLInputElement

@Suppress("FunctionName")
fun Tag.AttendeeEditPage(
    id: String?,
) = comp(
    AttendeeEditPage.Props(id = id)
) {
    AttendeeEditPage(it)
}

class AttendeeEditPage(ctx: Ctx<Props>) : Component<AttendeeEditPage.Props>(ctx) {

    //  PROPS  ////////////////////////////////////////////////////////////////////////////////////////////////////////

    data class Props(
        val id: String?,
    )

    //  STATE  ////////////////////////////////////////////////////////////////////////////////////////////////////////

    private val attendeeId: String get() = props.id ?: "_new_"
    private val isNew: Boolean get() = attendeeId == "_new_"

    data class Draft(
        val name: String = "",
        val email: String = "",
        val ticketType: TicketType = TicketType.Standard,
        val checkedIn: Boolean = false,
    )

    data class State(
        val original: Draft = Draft(),
        val draft: Draft = original,
    ) {
        val isChanged: Boolean get() = draft != original
    }

    private val formCtrl = formController()
    private val noDblClick = doubleClickProtection()

    private val loader = dataLoader {
        if (!isNew) {
            Apis.funktorConf.getAttendee(attendeeId).map { it.data!! }.map {
                State(original = Draft(it.name, it.email, it.ticketType, it.checkedIn))
            }
        } else {
            flowOf(State())
        }
    }

    private fun modifyDraft(block: Draft.() -> Draft) {
        loader.modifyValue { it.copy(draft = block(it.draft)) }
    }

    //  IMPL  /////////////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        ui.segment {
            ui.header H1 {
                icon.users()
                noui.content {
                    +(if (isNew) "New Attendee" else "Edit Attendee")
                }
            }

            ui.button {
                onClick { evt -> router.navToUri(evt, Nav.funktorConf.attendees()) }
                icon.arrow_left()
                +"Back to Attendees"
            }
        }

        loader.renderDefault(this) { data ->
            renderForm(data)
        }
    }

    private fun FlowContent.renderForm(state: State) {
        val draft = state.draft

        ui.segment {
            ui.form {
                UiInputField(draft.name, { modifyDraft { copy(name = it) } }) {
                    label("Name")
                    accepts(notBlank())
                }

                UiInputField(draft.email, { modifyDraft { copy(email = it) } }) {
                    label("Email")
                    accepts(notBlank())
                }

                SelectField(draft.ticketType, { modifyDraft { copy(ticketType = it) } }) {
                    label("Ticket Type")
                    TicketType.entries.forEach { t ->
                        option(t, t.name)
                    }
                }

                noui.field {
                    ui.toggle.checkbox {
                        checkBoxInput {
                            checked = draft.checkedIn
                            onChange { modifyDraft { copy(checkedIn = (it.target as HTMLInputElement).checked) } }
                        }
                        noui.label { +"Checked In" }
                    }
                }
            }

            ui.divider {}

            val canSave = formCtrl.isValid && noDblClick.canRun && state.isChanged

            ui.primary.givenNot(canSave) { disabled }
                .givenNot(noDblClick.canRun) { loading }
                .button {
                    onClick {
                        if (formCtrl.validate()) {
                            launch { saveAttendee(draft) }
                        }
                    }

                    +"Save"
                }
        }
    }

    private suspend fun saveAttendee(draft: Draft) = noDblClick.runBlocking {
        val request = SaveAttendeeRequest(
            name = draft.name,
            email = draft.email,
            ticketType = draft.ticketType,
            checkedIn = draft.checkedIn,
        )

        try {
            val response = if (isNew) {
                Apis.funktorConf.createAttendee(request).first()
            } else {
                Apis.funktorConf.updateAttendee(attendeeId, request).first()
            }

            if (response.isSuccess()) {
                toasts.info("Attendee saved successfully")
                router.navToUri(Nav.funktorConf.attendees())
            } else {
                toasts.error("Failed to save attendee")
            }
        } catch (e: Exception) {
            toasts.error("Failed to save attendee: ${e.message}")
        }
    }
}
