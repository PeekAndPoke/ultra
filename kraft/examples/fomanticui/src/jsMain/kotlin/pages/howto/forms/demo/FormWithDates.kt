@file:Suppress(
    "DuplicatedCode",
    "Detekt:TooManyFunctions",
    "Detekt:LargeClass",
    "Detekt:LongMethod",
)

package de.peekandpoke.kraft.examples.fomanticui.pages.howto.forms.demo

import de.peekandpoke.kraft.addons.forms.formController
import de.peekandpoke.kraft.addons.semanticui.forms.UiDateField
import de.peekandpoke.kraft.components.NoProps
import de.peekandpoke.kraft.components.PureComponent
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.components.onClick
import de.peekandpoke.kraft.examples.fomanticui.helpers.invoke
import de.peekandpoke.kraft.examples.fomanticui.helpers.renderStateAndDraftTable
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.vdom.VDom
import de.peekandpoke.ultra.common.datetime.*
import kotlinx.html.Tag

@Suppress("FunctionName")
fun Tag.FormWithDates() = comp {
    FormWithDates(it)
}

class FormWithDates(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class State(
        val localDate: MpLocalDate = Kronos.systemUtc.localDateTimeNow().toDate(),
        val localDateTime: MpLocalDateTime = Kronos.systemUtc.localDateTimeNow(),
        val zonedDateTime: MpZonedDateTime = Kronos.systemUtc.zonedDateTimeNow(MpTimezone.of("Europe/Berlin")),
    )

    private var state by value(State())
    private var draft by value(state)

    private val formCtrl = formController()

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {

        ui.two.column.grid {
            ui.column {
                ui.form {
                    ui.three.fields {
                        UiDateField(draft.localDate, { draft = draft.copy(localDate = it) }) {
                            label { +State::localDate.name }
                        }

                        UiDateField(draft.localDateTime, { draft = draft.copy(localDateTime = it) }) {
                            label { +State::localDateTime.name }
                        }

                        UiDateField(draft.zonedDateTime, { draft = draft.copy(zonedDateTime = it) }) {
                            label { +State::zonedDateTime.name }
                        }
                    }
                }

                ui.divider {}

                val canSubmit = formCtrl.isValid && draft != state

                ui.blue.button.given(!canSubmit) { disabled }.then {
                    onClick {
                        if (formCtrl.validate() && canSubmit) {
                            state = draft
                        }
                    }
                    +"Submit"
                }

                ui.basic.button {
                    onClick {
                        draft = state
                        formCtrl.resetAllFields()
                    }
                    +"Reset form"
                }
            }

            ui.column {
                renderStateAndDraftTable(
                    state,
                    draft,
                    listOf(
                        State::localDate { it.toIsoString() },
                        State::localDateTime { it.toIsoString() },
                        State::zonedDateTime { it.toIsoString() },
                    )
                )
            }
        }
    }
}
