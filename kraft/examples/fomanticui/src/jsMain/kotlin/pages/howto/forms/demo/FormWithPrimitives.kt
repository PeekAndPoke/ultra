@file:Suppress(
    "DuplicatedCode",
    "Detekt:TooManyFunctions",
    "Detekt:LargeClass",
    "Detekt:LongMethod",
)

package de.peekandpoke.kraft.examples.fomanticui.pages.howto.forms.demo

import de.peekandpoke.kraft.components.NoProps
import de.peekandpoke.kraft.components.PureComponent
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.examples.fomanticui.helpers.invoke
import de.peekandpoke.kraft.examples.fomanticui.helpers.renderStateAndDraftTable
import de.peekandpoke.kraft.forms.formController
import de.peekandpoke.kraft.semanticui.forms.UiCheckboxField
import de.peekandpoke.kraft.semanticui.forms.UiInputField
import de.peekandpoke.kraft.utils.responsiveCtrl
import de.peekandpoke.kraft.vdom.VDom
import de.peekandpoke.ultra.html.onClick
import de.peekandpoke.ultra.semanticui.icon
import de.peekandpoke.ultra.semanticui.ui
import kotlinx.html.FlowContent
import kotlinx.html.Tag

@Suppress("FunctionName")
fun Tag.FormWithPrimitives() = comp {
    FormWithPrimitives(it)
}

class FormWithPrimitives(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class State(
        val textInput: String = "",
        val intInput: Int = 0,
        val floatInput: Float = 0.0f,
        val doubleInput: Double = 3.0,
    )

    private var state by value(State())
    private var draft by value(state)
    private var disableFields by value(false)

    private val formCtrl = formController()

    private val responsive by subscribingTo(responsiveCtrl)

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {

        ui.two.column.grid {
            ui.column {
                ui.form {
                    ui.two.fields {
                        UiCheckboxField(::disableFields) {
                            toggle()
                            label("Disable fields")
                        }
                    }
                    ui.two.fields {
                        UiInputField(draft.textInput, { draft = draft.copy(textInput = it) }) {
                            label { +"Text Input" }
                            placeholder("Enter some text")
                            disabled(disableFields)

                            autofocusOnDesktop(responsive)
                            rightClearingIcon()

                            accepts(
                                _root_ide_package_.de.peekandpoke.kraft.forms.validation.strings.notBlank(),
                                _root_ide_package_.de.peekandpoke.kraft.forms.validation.comparable.greaterThan("abc") { "Must be greater than 'abc'" },
                            )
                        }

                        UiInputField(draft.intInput, { draft = draft.copy(intInput = it) }) {
                            label { +"Int Input" }
                            placeholder("Enter a number")
                            disabled(disableFields)
                            step(3)
                            leftIcon { green.percent }

                            accepts(
                                _root_ide_package_.de.peekandpoke.kraft.forms.validation.numbers.greaterThan(6.0),
                                _root_ide_package_.de.peekandpoke.kraft.forms.validation.numbers.lessThan(20.0)
                            )
                        }
                    }

                    ui.two.fields {
                        UiInputField(draft.floatInput, { draft = draft.copy(floatInput = it) }) {
                            label { +"Float Input" }
                            placeholder("Enter a number")
                            disabled(disableFields)
                            step(1.5)
                            rightLabel {
                                ui.blue.label {
                                    icon.dollar_sign()
                                    +"USD"
                                }
                            }

                            accepts(
                                _root_ide_package_.de.peekandpoke.kraft.forms.validation.numbers.greaterThan(5.0),
                                _root_ide_package_.de.peekandpoke.kraft.forms.validation.numbers.lessThan(20.0)
                            )
                        }

                        UiInputField(draft.doubleInput, { draft = draft.copy(doubleInput = it) }) {
                            label { +"Double Input" }
                            placeholder("Enter a number")
                            disabled(disableFields)
                            step(0.5)
                            leftLabel {
                                ui.label {
                                    icon.euro_sign()
                                    +"EUR"
                                }
                            }

                            accepts(
                                _root_ide_package_.de.peekandpoke.kraft.forms.validation.numbers.greaterThan(3.0),
                                _root_ide_package_.de.peekandpoke.kraft.forms.validation.numbers.lessThan(20.0)
                            )
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
                renderDataTable()
            }
        }
    }

    private fun FlowContent.renderDataTable() {

        renderStateAndDraftTable(
            state,
            draft,
            listOf(
                State::textInput { it },
                State::intInput { it.toString() },
                State::floatInput { it.toString() },
                State::doubleInput { it.toString() },
            )
        )
    }
}
