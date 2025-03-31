@file:Suppress(
    "DuplicatedCode",
    "Detekt:TooManyFunctions",
    "Detekt:LargeClass",
    "Detekt:LongMethod",
)

package de.peekandpoke.kraft.examples.fomanticui.pages.howto.forms.demo

import de.peekandpoke.kraft.addons.forms.formController
import de.peekandpoke.kraft.addons.semanticui.forms.UiDateField
import de.peekandpoke.kraft.addons.semanticui.forms.UiTimeField
import de.peekandpoke.kraft.components.NoProps
import de.peekandpoke.kraft.components.PureComponent
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.components.onClick
import de.peekandpoke.kraft.examples.fomanticui.helpers.invoke
import de.peekandpoke.kraft.examples.fomanticui.helpers.renderStateAndDraftTable
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.vdom.VDom
import de.peekandpoke.ultra.common.datetime.Kronos
import de.peekandpoke.ultra.common.datetime.MpLocalDateTime
import de.peekandpoke.ultra.common.datetime.MpLocalTime
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

        ui.two.column.grid {
            ui.column {
                ui.form {
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

//                        UiDateField(draft.zonedDateTime, modifyZonedDateTime) {
//                            label { +State::zonedDateTime.name }
//                        }
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
                        State::time { it.toIsoString() },
                        State::datetime { it.toIsoString() },
                    )
                )
            }
        }
    }
}
