@file:Suppress(
    "DuplicatedCode",
    "Detekt:TooManyFunctions",
    "Detekt:LargeClass",
    "Detekt:LongMethod",
)

package io.peekandpoke.kraft.examples.fomanticui.pages.elements.input

import generated.ExtractedCodeBlocks
import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.examples.fomanticui.helpers.HorizontalContentAndCode
import io.peekandpoke.kraft.examples.fomanticui.helpers.example
import io.peekandpoke.kraft.examples.fomanticui.helpers.readTheFomanticUiDocs
import io.peekandpoke.kraft.routing.PageTitle
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.semanticui.icon
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.html.FlowContent
import kotlinx.html.InputType
import kotlinx.html.Tag
import kotlinx.html.input
import kotlinx.html.p

@Suppress("FunctionName")
fun Tag.InputPage() = comp {
    InputPage(it)
}

class InputPage(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        PageTitle("Elements | Input")

        ui.basic.segment {
            ui.dividing.header H1 { +"Input" }

            readTheFomanticUiDocs("https://fomantic-ui.com/elements/input.html")

            ui.dividing.header H2 { +"Types" }

            renderInput()

            ui.dividing.header H2 { +"States" }

            renderFocus()
            renderLoading()
            renderDisabled()
            renderError()

            ui.dividing.header H2 { +"Variations" }

            renderIcon()
            renderLabeled()
            renderAction()
            renderTransparent()
            renderInverted()
            renderFluid()
            renderSize()
        }
    }

    private fun FlowContent.renderInput() = example {
        ui.header H3 { +"Input" }

        p { +"A standard input." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_input_InputPage_kt_renderInput,
        ) {
            // <CodeBlock renderInput>
            ui.input {
                input(type = InputType.text) {
                    placeholder = "Search..."
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderFocus() = example {
        ui.header H3 { +"Focus" }

        p { +"An input field can show a user is currently interacting with it." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_input_InputPage_kt_renderFocus,
        ) {
            // <CodeBlock renderFocus>
            ui.input.focus {
                input(type = InputType.text) {
                    placeholder = "Search..."
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderLoading() = example {
        ui.header H3 { +"Loading" }

        p { +"An icon input field can show that it is currently loading data." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_input_InputPage_kt_renderLoading,
        ) {
            // <CodeBlock renderLoading>
            ui.left.icon.loading.input {
                input(type = InputType.text) {
                    placeholder = "Search..."
                }
                icon.search()
            }

            ui.icon.loading.input {
                input(type = InputType.text) {
                    placeholder = "Search..."
                }
                icon.search()
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderDisabled() = example {
        ui.header H3 { +"Disabled" }

        p { +"An input field can show that it is disabled." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_input_InputPage_kt_renderDisabled,
        ) {
            // <CodeBlock renderDisabled>
            ui.disabled.input {
                input(type = InputType.text) {
                    placeholder = "Search..."
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderError() = example {
        ui.header H3 { +"Error" }

        p { +"An input field can show the data contains errors." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_input_InputPage_kt_renderError,
        ) {
            // <CodeBlock renderError>
            ui.error.input {
                input(type = InputType.text) {
                    placeholder = "Search..."
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderIcon() = example {
        ui.header H3 { +"Icon" }

        p { +"An input can be formatted with an icon." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_input_InputPage_kt_renderIcon,
        ) {
            // <CodeBlock renderIcon>
            ui.icon.input {
                input(type = InputType.text) {
                    placeholder = "Search..."
                }
                icon.search()
            }

            ui.left.icon.input {
                input(type = InputType.text) {
                    placeholder = "Search users..."
                }
                icon.users()
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderLabeled() = example {
        ui.header H3 { +"Labeled" }

        p { +"An input can be formatted with a label." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_input_InputPage_kt_renderLabeled,
        ) {
            // <CodeBlock renderLabeled>
            ui.labeled.input {
                ui.label { +"https://" }
                input(type = InputType.text) {
                    placeholder = "mysite.com"
                }
            }

            ui.right.labeled.input {
                input(type = InputType.text) {
                    placeholder = "Find domain"
                }
                ui.label { +".com" }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderAction() = example {
        ui.header H3 { +"Action" }

        p { +"An input can be formatted to alert the user to an action they may perform." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_input_InputPage_kt_renderAction,
        ) {
            // <CodeBlock renderAction>
            ui.action.input {
                input(type = InputType.text) {
                    placeholder = "Search..."
                }
                ui.button { +"Search" }
            }

            ui.left.action.input {
                ui.teal.labeled.icon.button {
                    icon.shopping_cart()
                    +"Checkout"
                }
                input(type = InputType.text) {
                    value = "$52.03"
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderTransparent() = example {
        ui.header H3 { +"Transparent" }

        p { +"A transparent input has no background." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_input_InputPage_kt_renderTransparent,
        ) {
            // <CodeBlock renderTransparent>
            ui.transparent.input {
                input(type = InputType.text) {
                    placeholder = "Search..."
                }
            }

            ui.transparent.icon.input {
                input(type = InputType.text) {
                    placeholder = "Search..."
                }
                icon.search()
            }

            ui.transparent.left.icon.input {
                input(type = InputType.text) {
                    placeholder = "Search..."
                }
                icon.search()
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderInverted() = example {
        ui.header H3 { +"Inverted" }

        p { +"An input can be formatted to appear on dark backgrounds." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_input_InputPage_kt_renderInverted,
        ) {
            // <CodeBlock renderInverted>
            ui.inverted.segment {
                ui.inverted.input {
                    input(type = InputType.text) {
                        placeholder = "Search..."
                    }
                }

                ui.inverted.divider {}

                ui.inverted.left.icon.input {
                    input(type = InputType.text) {
                        placeholder = "Search..."
                    }
                    icon.search()
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderFluid() = example {
        ui.header H3 { +"Fluid" }

        p { +"An input can take the size of its container." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_input_InputPage_kt_renderFluid,
        ) {
            // <CodeBlock renderFluid>
            ui.fluid.input {
                input(type = InputType.text) {
                    placeholder = "Search a very wide input..."
                }
            }

            ui.fluid.icon.input {
                input(type = InputType.text) {
                    placeholder = "Search a very wide input..."
                }
                icon.search()
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderSize() = example {
        ui.header H3 { +"Size" }

        p { +"An input can vary in size." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_input_InputPage_kt_renderSize,
        ) {
            // <CodeBlock renderSize>
            ui.mini.icon.input {
                input(type = InputType.text) { placeholder = "Search mini..." }
                icon.search()
            }

            ui.divider {}

            ui.small.icon.input {
                input(type = InputType.text) { placeholder = "Search small..." }
                icon.search()
            }

            ui.divider {}

            ui.large.icon.input {
                input(type = InputType.text) { placeholder = "Search large..." }
                icon.search()
            }

            ui.divider {}

            ui.big.icon.input {
                input(type = InputType.text) { placeholder = "Search big..." }
                icon.search()
            }

            ui.divider {}

            ui.huge.icon.input {
                input(type = InputType.text) { placeholder = "Search huge..." }
                icon.search()
            }

            ui.divider {}

            ui.massive.icon.input {
                input(type = InputType.text) { placeholder = "Search massive..." }
                icon.search()
            }
            // </CodeBlock>
        }
    }
}
