@file:Suppress(
    "DuplicatedCode",
    "Detekt:TooManyFunctions",
    "Detekt:LargeClass",
    "Detekt:LongMethod",
)

package io.peekandpoke.kraft.examples.fomanticui.pages.howto.forms.demo

import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.examples.fomanticui.helpers.invoke
import io.peekandpoke.kraft.examples.fomanticui.helpers.renderStateAndDraftTable
import io.peekandpoke.kraft.forms.formController
import io.peekandpoke.kraft.semanticui.forms.UiPasswordField
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.html.onClick
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.html.Tag

@Suppress("FunctionName")
fun Tag.FormWithPasswords() = comp {
    FormWithPasswords(it)
}

class FormWithPasswords(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class State(
        val password: String = "",
        val repeat: String = "",
        val reveal: String = "bad-password",
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
                        UiPasswordField(draft.password, { draft = draft.copy(password = it) }) {
                            label { +"Password" }
                        }

                        UiPasswordField(draft.repeat, { draft = draft.copy(repeat = it) }) {
                            label { +"Repeat" }

                            accepts(
                                _root_ide_package_.io.peekandpoke.kraft.forms.validation.equalTo(
                                    { draft.password },
                                    "Passwords must match"
                                )
                            )
                        }

                        UiPasswordField(draft.reveal, { draft = draft.copy(reveal = it) }) {
                            label { +"Reveal" }

                            revealPasswordIcon()

                            accepts(
                                _root_ide_package_.io.peekandpoke.kraft.forms.validation.equalTo(
                                    { draft.password },
                                    "Passwords must match"
                                )
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
                renderStateAndDraftTable(
                    state,
                    draft,
                    listOf(
                        State::password { it },
                        State::repeat { it },
                    )
                )
            }
        }
    }
}
