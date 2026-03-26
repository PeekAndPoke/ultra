@file:Suppress(
    "DuplicatedCode",
    "Detekt:TooManyFunctions",
    "Detekt:LargeClass",
    "Detekt:LongMethod",
)

package io.peekandpoke.kraft.examples.fomanticui.pages.howto.forms.demo

import generated.ExtractedCodeBlocks
import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.examples.fomanticui.helpers.CodePanelColumn
import io.peekandpoke.kraft.examples.fomanticui.helpers.invoke
import io.peekandpoke.kraft.examples.fomanticui.helpers.renderStateAndDraftTable
import io.peekandpoke.kraft.forms.formController
import io.peekandpoke.kraft.semanticui.forms.UiDateField
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.datetime.MpLocalDate
import io.peekandpoke.ultra.datetime.MpLocalDateTime
import io.peekandpoke.ultra.datetime.MpTimezone
import io.peekandpoke.ultra.datetime.MpZonedDateTime
import io.peekandpoke.ultra.html.onClick
import io.peekandpoke.ultra.semanticui.ui
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

        ui.stackable.two.column.grid {
            ui.column {
                ui.form {
                    // <CodeBlock nullableDates>
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
                    // </CodeBlock>
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

                ui.divider {}

                renderStateAndDraftTable(
                    state, draft, listOf(
                        State::localDate { it?.toIsoString() },
                        State::localDateTime { it?.toIsoString() },
                        State::zonedDateTime { it?.toIsoString() },
                    )
                )
            }

            CodePanelColumn(ExtractedCodeBlocks.pages_howto_forms_demo_FormWithNullableDates_kt_nullableDates)
        }
    }
}
