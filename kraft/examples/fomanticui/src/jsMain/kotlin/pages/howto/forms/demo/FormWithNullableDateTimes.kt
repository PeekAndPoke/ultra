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
import io.peekandpoke.kraft.forms.validation.nonNull
import io.peekandpoke.kraft.semanticui.forms.UiDateTimeField
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.datetime.MpLocalDateTime
import io.peekandpoke.ultra.datetime.MpTimezone
import io.peekandpoke.ultra.datetime.MpZonedDateTime
import io.peekandpoke.ultra.html.onClick
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.html.Tag

@Suppress("FunctionName")
fun Tag.FormWithNullableDateTimes() = comp {
    FormWithNullableDateTimes(it)
}

class FormWithNullableDateTimes(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class State(
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
                    // <CodeBlock nullableDateTimes>
                    ui.two.fields {
                        UiDateTimeField.nullable(draft.localDateTime, { draft = draft.copy(localDateTime = it) }) {
                            label { +State::localDateTime.name }
                            accepts(nonNull())
                        }

                        val tz = MpTimezone.of("Europe/Berlin")

                        UiDateTimeField.nullable(draft.zonedDateTime, tz, { draft = draft.copy(zonedDateTime = it) }) {
                            label { +State::zonedDateTime.name }
                            accepts(nonNull())
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
                        State::localDateTime { it?.toIsoString() },
                        State::zonedDateTime { it?.toIsoString() },
                    )
                )
            }

            CodePanelColumn(ExtractedCodeBlocks.pages_howto_forms_demo_FormWithNullableDateTimes_kt_nullableDateTimes)
        }
    }
}
