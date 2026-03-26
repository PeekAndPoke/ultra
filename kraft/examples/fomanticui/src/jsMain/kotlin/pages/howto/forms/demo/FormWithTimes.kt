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
import io.peekandpoke.kraft.semanticui.forms.UiTimeField
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.datetime.Kronos
import io.peekandpoke.ultra.datetime.MpLocalDateTime
import io.peekandpoke.ultra.datetime.MpLocalTime
import io.peekandpoke.ultra.html.onClick
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.html.Tag

@Suppress("FunctionName")
fun Tag.FormWithTimes() = comp {
    FormWithTimes(it)
}

class FormWithTimes(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class State(
        val time: MpLocalTime = Kronos.systemUtc.localDateTimeNow().toTime(),
        val datetime: MpLocalDateTime = Kronos.systemUtc.localDateTimeNow(),
    )

    private var state by value(State())
    private var draft by value(state)

    private fun <P> modify(block: State.(P) -> State): (P) -> Unit = { draft = draft.block(it) }

    private val modifyTime = modify<MpLocalTime> { copy(time = it) }
    private val modifyDateTime = modify<MpLocalDateTime> { copy(datetime = it) }

    private val formCtrl = formController()

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {

        ui.stackable.two.column.grid {
            ui.column {
                ui.form {
                    // <CodeBlock times>
                    ui.three.fields {
                        UiTimeField(draft.time, modifyTime) {
                            label { +State::time.name }
                        }

                        UiDateField(
                            value = draft.datetime.toDate(),
                            onChange = { modifyDateTime(it.atTime(draft.datetime.toTime())) }
                        ) {
                            label { +"${State::datetime.name} - date" }
                        }

                        UiTimeField(
                            value = draft.datetime.toTime(),
                            onChange = { modifyDateTime(draft.datetime.toDate().atTime(it)) }
                        ) {
                            label { +"${State::datetime.name} - time" }
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
                        State::time { it.toIsoString() },
                        State::datetime { it.toIsoString() },
                    )
                )
            }

            CodePanelColumn(ExtractedCodeBlocks.pages_howto_forms_demo_FormWithTimes_kt_times)
        }
    }
}
