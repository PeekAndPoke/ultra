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
import io.peekandpoke.kraft.semanticui.forms.UiCheckboxField
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.html.onClick
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.html.Tag

@Suppress("FunctionName")
fun Tag.FormWithCheckboxes() = comp {
    FormWithCheckboxes(it)
}

class FormWithCheckboxes(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class Obj(val x: String)

    data class State(
        val boolean: Boolean = false,
        val string: String = "yes",
        val obj: Obj = Obj("yes"),
    )

    private var state by value(State())
    private var draft by value(state)

    private val formCtrl = formController()

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {

        ui.stackable.two.column.grid {
            ui.column {
                ui.form {
                    // <CodeBlock checkboxes>
                    ui.three.fields {
                        UiCheckboxField(draft.boolean, { draft = draft.copy(boolean = it) }) {
                            label { +State::boolean.name }
                        }

                        UiCheckboxField(
                            value = draft.string,
                            off = "no",
                            on = "yes",
                            onChange = { draft = draft.copy(string = it) },
                        ) {
                            label { +State::string.name }
                            toggle()
                        }

                        UiCheckboxField(
                            value = draft.obj,
                            off = Obj("no"),
                            on = Obj("yes"),
                            onChange = { draft = draft.copy(obj = it) },
                        ) {
                            label { +State::obj.name }
                            slider()
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
                        State::boolean { it.toString() },
                        State::string { it },
                        State::obj { it.toString() },
                    )
                )
            }

            CodePanelColumn(ExtractedCodeBlocks.pages_howto_forms_demo_FormWithCheckboxes_kt_checkboxes)
        }
    }
}
