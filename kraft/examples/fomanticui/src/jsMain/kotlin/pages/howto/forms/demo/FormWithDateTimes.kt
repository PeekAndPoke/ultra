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
import io.peekandpoke.kraft.semanticui.forms.UiDateTimeField
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.datetime.Kronos
import io.peekandpoke.ultra.datetime.MpLocalDateTime
import io.peekandpoke.ultra.datetime.MpTimezone
import io.peekandpoke.ultra.datetime.MpZonedDateTime
import io.peekandpoke.ultra.html.onClick
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.html.Tag

@Suppress("FunctionName")
fun Tag.FormWithDateTimes() = comp {
    FormWithDateTimes(it)
}

class FormWithDateTimes(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class State(
        val localDateTime: MpLocalDateTime =
            Kronos.systemUtc.localDateTimeNow(),
        val zonedDateTime: MpZonedDateTime =
            Kronos.systemUtc.zonedDateTimeNow(MpTimezone.of("Europe/Berlin")).atStartOfHour(),
    )

    private var state by value(State())
    private var draft by value(state)

    private val formCtrl = formController()

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {

        ui.stackable.two.column.grid {
            ui.column {
                ui.form {
                    // <CodeBlock dateTimes>
                    ui.two.fields {
                        UiDateTimeField(draft.localDateTime, { draft = draft.copy(localDateTime = it) }) {
                            label { +State::localDateTime.name }
                        }

                        UiDateTimeField(draft.zonedDateTime, { draft = draft.copy(zonedDateTime = it) }) {
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
                    state,
                    draft,
                    listOf(
                        State::localDateTime { it.toIsoString() },
                        State::zonedDateTime { it.toIsoString() },
                    )
                )
            }

            CodePanelColumn(ExtractedCodeBlocks.pages_howto_forms_demo_FormWithDateTimes_kt_dateTimes)
        }
    }
}
