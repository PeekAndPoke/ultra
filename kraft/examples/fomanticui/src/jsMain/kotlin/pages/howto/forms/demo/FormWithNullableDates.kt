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
import de.peekandpoke.ultra.common.datetime.MpLocalDate
import de.peekandpoke.ultra.common.datetime.MpLocalDateTime
import de.peekandpoke.ultra.common.datetime.MpTimezone
import de.peekandpoke.ultra.common.datetime.MpZonedDateTime
import kotlinx.html.Tag

@Suppress("FunctionName")
fun Tag.FormWithNullableDates() = comp {
    FormWithNullableDates(it)
}

class FormWithNullableDates(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class State(
        val localDate: MpLocalDate? = null,
        val localDateTime: MpLocalDateTime? = null,
        val zonedDateTime: MpZonedDateTime? = null,
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
                        UiDateField.nullable(draft.localDate, { draft = draft.copy(localDate = it) }) {
                            label { +State::localDate.name }
                        }

                        UiDateField.nullable(draft.localDateTime, { draft = draft.copy(localDateTime = it) }) {
                            label { +State::localDateTime.name }
                        }

                        val tz = MpTimezone.of("Europe/Berlin")

                        UiDateField.nullable(draft.zonedDateTime, tz, { draft = draft.copy(zonedDateTime = it) }) {
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
                    state, draft, listOf(
                        State::localDate { it?.toIsoString() },
                        State::localDateTime { it?.toIsoString() },
                        State::zonedDateTime { it?.toIsoString() },
                    )
                )
            }
        }
    }
}
