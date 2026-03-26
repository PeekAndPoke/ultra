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
import io.peekandpoke.kraft.forms.validation.numbers.greaterThan
import io.peekandpoke.kraft.forms.validation.numbers.lessThan
import io.peekandpoke.kraft.semanticui.forms.UiInputField
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.html.onClick
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.html.FlowContent
import kotlinx.html.Tag

@Suppress("FunctionName")
fun Tag.FormWithNullablePrimitives() = comp {
    FormWithNullablePrimitives(it)
}

class FormWithNullablePrimitives(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class State(
        val textInput: String? = null,
        val intInput: Int? = null,
        val floatInput: Float? = null,
        val doubleInput: Double? = null,
    )

    private var state by value(State())
    private var draft by value(state)

    private val formCtrl = formController()

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {

        ui.stackable.two.column.grid {
            ui.column {
                ui.form {
                    // <CodeBlock nullablePrimitives>
                    ui.two.fields {
                        UiInputField.nullable(draft.textInput, { draft = draft.copy(textInput = it) }) {
                            label { +"Text Input" }
                            placeholder("Enter some text")
                        }

                        UiInputField.nullable(draft.intInput, { draft = draft.copy(intInput = it) }) {
                            label { +"Int Input" }
                            placeholder("Enter a number")
                            step(3)

                            accepts(
                                greaterThan(6.0),
                                lessThan(15.0)
                            )
                        }
                    }

                    ui.two.fields {
                        UiInputField.nullable(draft.floatInput, { draft = draft.copy(floatInput = it) }) {
                            label { +"Float Input" }
                            placeholder("Enter a number")

                            accepts(
                                greaterThan(5.0),
                                lessThan(20.0)
                            )
                        }

                        UiInputField.nullable(draft.doubleInput, { draft = draft.copy(doubleInput = it) }) {
                            label { +"Double Input" }
                            placeholder("Enter a number")
                            step(0.5)

                            accepts(
                                greaterThan(3.0),
                                lessThan(10.0)
                            )
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

                renderDataTable()
            }

            CodePanelColumn(ExtractedCodeBlocks.pages_howto_forms_demo_FormWithNullablePrimitives_kt_nullablePrimitives)
        }
    }

    private fun FlowContent.renderDataTable() {

        renderStateAndDraftTable(
            state,
            draft,
            listOf(
                State::textInput { it.toString() },
                State::intInput { it.toString() },
                State::floatInput { it.toString() },
                State::doubleInput { it.toString() },
            )
        )
    }
}
