@file:Suppress(
    "DuplicatedCode",
    "Detekt:TooManyFunctions",
    "Detekt:LargeClass",
    "Detekt:LongMethod",
)

package de.peekandpoke.kraft.examples.fomanticui.pages.howto.forms.demo

import de.peekandpoke.kraft.addons.forms.formController
import de.peekandpoke.kraft.addons.semanticui.forms.UiCheckboxField
import de.peekandpoke.kraft.addons.semanticui.forms.UiInputField
import de.peekandpoke.kraft.addons.semanticui.forms.UiTextArea
import de.peekandpoke.kraft.addons.semanticui.forms.old.select.SelectField
import de.peekandpoke.kraft.addons.semanticui.forms.old.select.SelectFieldComponent
import de.peekandpoke.kraft.components.NoProps
import de.peekandpoke.kraft.components.PureComponent
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.components.onClick
import de.peekandpoke.kraft.examples.fomanticui.helpers.invoke
import de.peekandpoke.kraft.examples.fomanticui.helpers.renderStateAndDraftTable
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.vdom.VDom
import kotlinx.coroutines.flow.flowOf
import kotlinx.html.InputType
import kotlinx.html.Tag

@Suppress("FunctionName")
fun Tag.FormWithDisabledFields() = comp {
    FormWithDisabledFields(it)
}

class FormWithDisabledFields(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class State(
        val input: String = "This a disabled input field",
        val textarea: String = "This is a disabled textarea",
        val checkbox: Boolean = true,
        val select: String = "Select",
        val suggest: String = "Suggest",
        val color: String = "#FFFFFF",
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
                        UiCheckboxField(draft.checkbox, { draft = draft.copy(checkbox = it) }) {
                            label { +State::checkbox.name }
                            toggle()
                            disabled()
                        }

                        UiInputField(draft.input, { draft = draft.copy(input = it) }) {
                            label { +State::input.name }
                            disabled()
                        }

                        UiTextArea(draft.textarea, { draft = draft.copy(textarea = it) }) {
                            label { +State::textarea.name }
                            disabled()
                        }
                    }

                    ui.three.fields {
                        SelectField(draft.select, { draft = draft.copy(select = it) }) {
                            label { +State::select.name }
                            disabled()

                            option("Select", "Select")
                        }

                        SelectField(draft.suggest, { draft = draft.copy(suggest = it) }) {
                            label { +State::suggest.name }
                            disabled()

                            autoSuggest { _ ->
                                flowOf(
                                    listOf(
                                        SelectFieldComponent.Option("Suggest", "Suggest"),
                                        SelectFieldComponent.Option("A", "A"),
                                        SelectFieldComponent.Option("B", "B"),
                                        SelectFieldComponent.Option("C", "C"),
                                    )
                                )
                            }
                        }

                        UiInputField(draft.color, { draft = draft.copy(color = it) }) {
                            label { +State::color.name }
                            type(InputType.color)
                            disabled()
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
                        State::checkbox { it.toString() },
                        State::input { it },
                        State::textarea { it },
                        State::select { it },
                        State::suggest { it },
                    ),
                )
            }
        }
    }
}
