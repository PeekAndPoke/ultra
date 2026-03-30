package io.peekandpoke.funktor.demo.adminapp.pages.funktorconf

import io.peekandpoke.funktor.demo.adminapp.Apis
import io.peekandpoke.funktor.demo.adminapp.Nav
import io.peekandpoke.funktor.demo.common.funktorconf.EventStatus
import io.peekandpoke.funktor.demo.common.funktorconf.SaveEventRequest
import io.peekandpoke.funktor.inspect.renderDefault
import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.forms.formController
import io.peekandpoke.kraft.forms.validation.strings.notBlank
import io.peekandpoke.kraft.routing.Router.Companion.router
import io.peekandpoke.kraft.semanticui.forms.UiInputField
import io.peekandpoke.kraft.semanticui.forms.UiTextArea
import io.peekandpoke.kraft.semanticui.forms.old.select.SelectField
import io.peekandpoke.kraft.toasts.ToastsManager.Companion.toasts
import io.peekandpoke.kraft.utils.dataLoader
import io.peekandpoke.kraft.utils.doubleClickProtection
import io.peekandpoke.kraft.utils.launch
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.html.onClick
import io.peekandpoke.ultra.semanticui.icon
import io.peekandpoke.ultra.semanticui.noui
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.html.FlowContent
import kotlinx.html.Tag

@Suppress("FunctionName")
fun Tag.EventEditPage(
    id: String?,
) = comp(
    EventEditPage.Props(id = id)
) {
    EventEditPage(it)
}

class EventEditPage(ctx: Ctx<Props>) : Component<EventEditPage.Props>(ctx) {

    //  PROPS  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class Props(
        val id: String?,
    )

    //  STATE  ////////////////////////////////////////////////////////////////////////////////////////////////////////

    private val eventId: String get() = props.id ?: "_new_"
    private val isNew: Boolean get() = eventId == "_new_"

    data class Draft(
        val name: String = "",
        val description: String = "",
        val venue: String = "",
        val status: EventStatus = EventStatus.Draft,
        val startDate: String = "",
        val endDate: String = "",
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
            Apis.funktorConf.getEvent(eventId).map { it.data!! }.map {
                State(original = Draft(it.name, it.description, it.venue, it.status, it.startDate, it.endDate))
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
                icon.calendar()
                noui.content {
                    +(if (isNew) "New Event" else "Edit Event")
                }
            }

            ui.button {
                onClick { evt -> router.navToUri(evt, Nav.funktorConf.events()) }
                icon.arrow_left()
                +"Back to Events"
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

                UiTextArea(draft.description, { modifyDraft { copy(description = it) } }) {
                    label("Description")
                    accepts(notBlank())
                }

                UiInputField(draft.venue, { modifyDraft { copy(venue = it) } }) {
                    label("Venue")
                    accepts(notBlank())
                }

                noui.two.fields {
                    UiInputField(draft.startDate, { modifyDraft { copy(startDate = it) } }) {
                        label("Start Date")
                        accepts(notBlank())
                    }

                    UiInputField(draft.endDate, { modifyDraft { copy(endDate = it) } }) {
                        label("End Date")
                        accepts(notBlank())
                    }
                }

                SelectField(draft.status, { modifyDraft { copy(status = it) } }) {
                    label("Status")
                    EventStatus.entries.forEach { s ->
                        option(s, s.name)
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
                            launch { saveEvent(draft) }
                        }
                    }

                    +"Save"
                }
        }
    }

    private suspend fun saveEvent(draft: Draft) = noDblClick.runBlocking {
        val request = SaveEventRequest(
            name = draft.name,
            description = draft.description,
            venue = draft.venue,
            status = draft.status,
            startDate = draft.startDate,
            endDate = draft.endDate,
        )

        try {
            val response = if (isNew) {
                Apis.funktorConf.createEvent(request).first()
            } else {
                Apis.funktorConf.updateEvent(eventId, request).first()
            }

            if (response.isSuccess()) {
                toasts.info("Event saved successfully")
                router.navToUri(Nav.funktorConf.events())
            } else {
                toasts.error("Failed to save event")
            }
        } catch (e: Exception) {
            toasts.error("Failed to save event: ${e.message}")
        }
    }
}
