@file:Suppress(
    "DuplicatedCode",
    "Detekt:TooManyFunctions",
    "Detekt:LargeClass",
    "Detekt:LongMethod",
)

package io.peekandpoke.kraft.examples.fomanticui.pages.collections.form

import generated.ExtractedCodeBlocks
import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.examples.fomanticui.helpers.HorizontalContentAndCode
import io.peekandpoke.kraft.examples.fomanticui.helpers.example
import io.peekandpoke.kraft.examples.fomanticui.helpers.readTheFomanticUiDocs
import io.peekandpoke.kraft.routing.PageTitle
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.semanticui.noui
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.html.FlowContent
import kotlinx.html.InputType
import kotlinx.html.Tag
import kotlinx.html.input
import kotlinx.html.label
import kotlinx.html.p
import kotlinx.html.textArea

@Suppress("FunctionName")
fun Tag.FormPage() = comp {
    FormPage(it)
}

class FormPage(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        PageTitle("Collections | Form")

        ui.basic.segment {
            ui.dividing.header H1 { +"Form" }

            readTheFomanticUiDocs("https://fomantic-ui.com/collections/form.html")

            ui.dividing.header H2 { +"Types" }

            renderForm()

            ui.dividing.header H2 { +"Content" }

            renderField()
            renderFields()
            renderTextArea()
            renderCheckbox()
            renderMessage()

            ui.dividing.header H2 { +"Form States" }

            renderLoading()
            renderSuccess()
            renderError()
            renderWarning()

            ui.dividing.header H2 { +"Field States" }

            renderFieldError()
            renderDisabledField()
            renderReadOnlyField()

            ui.dividing.header H2 { +"Form Variations" }

            renderSize()
            renderEqualWidthForm()
            renderInverted()

            ui.dividing.header H2 { +"Field Variations" }

            renderInlineField()
            renderFieldWidth()
            renderRequiredField()

            ui.dividing.header H2 { +"Group Variations" }

            renderEqualWidthFields()
            renderGroupedFields()
            renderInlineFields()
        }
    }

    // region Types

    private fun FlowContent.renderForm() = example {
        ui.header H3 { +"Form" }

        p { +"A form" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_form_FormPage_kt_renderForm,
        ) {
            // <CodeBlock renderForm>
            ui.form {
                noui.field {
                    label { +"First Name" }
                    input(type = InputType.text) { placeholder = "First Name" }
                }
                noui.field {
                    label { +"Last Name" }
                    input(type = InputType.text) { placeholder = "Last Name" }
                }
                noui.field {
                    ui.checkbox {
                        input { type = InputType.checkBox }
                        label { +"I agree to the Terms and Conditions" }
                    }
                }
                ui.button { +"Submit" }
            }
            // </CodeBlock>
        }
    }

    // endregion

    // region Content

    private fun FlowContent.renderField() = example {
        ui.header H3 { +"Field" }

        p { +"A field is a form element containing a label and an input" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_form_FormPage_kt_renderField,
        ) {
            // <CodeBlock renderField>
            ui.form {
                noui.field {
                    label { +"User Input" }
                    input(type = InputType.text) { placeholder = "Enter value" }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderFields() = example {
        ui.header H3 { +"Fields" }

        p { +"A set of fields can appear grouped together" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_form_FormPage_kt_renderFields_1,
        ) {
            // <CodeBlock renderFields_1>
            ui.form {
                ui.two.fields {
                    noui.field {
                        label { +"First Name" }
                        input(type = InputType.text) { placeholder = "First Name" }
                    }
                    noui.field {
                        label { +"Last Name" }
                        input(type = InputType.text) { placeholder = "Last Name" }
                    }
                }
            }
            // </CodeBlock>
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_form_FormPage_kt_renderFields_2,
        ) {
            // <CodeBlock renderFields_2>
            ui.form {
                ui.three.fields {
                    noui.field {
                        label { +"First Name" }
                        input(type = InputType.text) { placeholder = "First Name" }
                    }
                    noui.field {
                        label { +"Middle Name" }
                        input(type = InputType.text) { placeholder = "Middle Name" }
                    }
                    noui.field {
                        label { +"Last Name" }
                        input(type = InputType.text) { placeholder = "Last Name" }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderTextArea() = example {
        ui.header H3 { +"Text Area" }

        p { +"A textarea can be used to allow for extended user input" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_form_FormPage_kt_renderTextArea,
        ) {
            // <CodeBlock renderTextArea>
            ui.form {
                noui.field {
                    label { +"Text" }
                    textArea { rows = "3" }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderCheckbox() = example {
        ui.header H3 { +"Checkbox" }

        p { +"A form can contain a checkbox, slider, or toggle" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_form_FormPage_kt_renderCheckbox_1,
        ) {
            // <CodeBlock renderCheckbox_1>
            ui.form {
                noui.field {
                    ui.checkbox {
                        input { type = InputType.checkBox }
                        label { +"Standard checkbox" }
                    }
                }
            }
            // </CodeBlock>
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_form_FormPage_kt_renderCheckbox_2,
        ) {
            // <CodeBlock renderCheckbox_2>
            ui.form {
                noui.field {
                    ui.slider.checkbox {
                        input { type = InputType.checkBox }
                        label { +"Slider checkbox" }
                    }
                }
                noui.field {
                    ui.toggle.checkbox {
                        input { type = InputType.checkBox }
                        label { +"Toggle checkbox" }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderMessage() = example {
        ui.header H3 { +"Message" }

        p { +"A form can contain a message which is hidden by default and shown on validation" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_form_FormPage_kt_renderMessage,
        ) {
            // <CodeBlock renderMessage>
            ui.form {
                ui.error.message {
                    noui.header { +"Action Forbidden" }
                    p { +"Please check the form for errors." }
                }
                ui.success.message {
                    noui.header { +"Form Completed" }
                    p { +"Your submission has been received." }
                }
                ui.warning.message {
                    noui.header { +"Could you check something?" }
                    p { +"That email is already taken." }
                }
            }
            // </CodeBlock>
        }
    }

    // endregion

    // region Form States

    private fun FlowContent.renderLoading() = example {
        ui.header H3 { +"Loading" }

        p { +"If a form is in loading state, it will automatically show a loading indicator." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_form_FormPage_kt_renderLoading,
        ) {
            // <CodeBlock renderLoading>
            ui.loading.form {
                noui.field {
                    label { +"Email" }
                    input(type = InputType.text) { placeholder = "joe@example.com" }
                }
                ui.button { +"Submit" }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderSuccess() = example {
        ui.header H3 { +"Success" }

        p { +"If a form is in a success state, it will automatically show any success message blocks." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_form_FormPage_kt_renderSuccess,
        ) {
            // <CodeBlock renderSuccess>
            ui.success.form {
                noui.field {
                    label { +"Email" }
                    input(type = InputType.text) { placeholder = "joe@example.com" }
                }
                ui.success.message {
                    noui.header { +"Form Completed" }
                    p { +"You're all signed up for the newsletter." }
                }
                ui.button { +"Submit" }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderError() = example {
        ui.header H3 { +"Error" }

        p { +"If a form is in an error state, it will automatically show any error message blocks." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_form_FormPage_kt_renderError,
        ) {
            // <CodeBlock renderError>
            ui.error.form {
                noui.field {
                    label { +"Email" }
                    input(type = InputType.text) { placeholder = "joe@example.com" }
                }
                ui.error.message {
                    noui.header { +"Action Forbidden" }
                    p { +"You can only sign up for an account once with a given e-mail address." }
                }
                ui.button { +"Submit" }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderWarning() = example {
        ui.header H3 { +"Warning" }

        p { +"If a form is in a warning state, it will automatically show any warning message blocks." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_form_FormPage_kt_renderWarning,
        ) {
            // <CodeBlock renderWarning>
            ui.warning.form {
                noui.field {
                    label { +"Email" }
                    input(type = InputType.text) { placeholder = "joe@example.com" }
                }
                ui.warning.message {
                    noui.header { +"Could you check something?" }
                    p { +"That e-mail has been deactivated." }
                }
                ui.button { +"Submit" }
            }
            // </CodeBlock>
        }
    }

    // endregion

    // region Field States

    private fun FlowContent.renderFieldError() = example {
        ui.header H3 { +"Field Error" }

        p { +"Individual fields may display an error state" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_form_FormPage_kt_renderFieldError,
        ) {
            // <CodeBlock renderFieldError>
            ui.form {
                ui.two.fields {
                    noui.field.error {
                        label { +"First Name" }
                        input(type = InputType.text) { placeholder = "First Name" }
                    }
                    noui.field {
                        label { +"Last Name" }
                        input(type = InputType.text) { placeholder = "Last Name" }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderDisabledField() = example {
        ui.header H3 { +"Disabled Field" }

        p { +"Individual fields may be disabled" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_form_FormPage_kt_renderDisabledField,
        ) {
            // <CodeBlock renderDisabledField>
            ui.form {
                ui.two.fields {
                    noui.disabled.field {
                        label { +"First Name" }
                        input(type = InputType.text) { placeholder = "Disabled" }
                    }
                    noui.field {
                        label { +"Last Name" }
                        input(type = InputType.text) { placeholder = "Last Name" }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderReadOnlyField() = example {
        ui.header H3 { +"Read-Only Field" }

        p { +"Individual fields may be read only" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_form_FormPage_kt_renderReadOnlyField,
        ) {
            // <CodeBlock renderReadOnlyField>
            ui.form {
                ui.two.fields {
                    noui.field {
                        label { +"First Name" }
                        input(type = InputType.text) { readonly = true; value = "Read Only Value" }
                    }
                    noui.field {
                        label { +"Last Name" }
                        input(type = InputType.text) { placeholder = "Last Name" }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    // endregion

    // region Form Variations

    private fun FlowContent.renderSize() = example {
        ui.header H3 { +"Size" }

        p { +"A form can vary in size" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_form_FormPage_kt_renderSize_1,
        ) {
            // <CodeBlock renderSize_1>
            ui.mini.form {
                ui.two.fields {
                    noui.field {
                        label { +"First Name" }
                        input(type = InputType.text) { placeholder = "First Name" }
                    }
                    noui.field {
                        label { +"Last Name" }
                        input(type = InputType.text) { placeholder = "Last Name" }
                    }
                }
                ui.button { +"Submit" }
            }
            // </CodeBlock>
        }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_form_FormPage_kt_renderSize_2,
        ) {
            // <CodeBlock renderSize_2>
            ui.huge.form {
                ui.two.fields {
                    noui.field {
                        label { +"First Name" }
                        input(type = InputType.text) { placeholder = "First Name" }
                    }
                    noui.field {
                        label { +"Last Name" }
                        input(type = InputType.text) { placeholder = "Last Name" }
                    }
                }
                ui.button { +"Submit" }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderEqualWidthForm() = example {
        ui.header H3 { +"Equal Width Form" }

        p { +"Forms can automatically divide fields to be equal width" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_form_FormPage_kt_renderEqualWidthForm,
        ) {
            // <CodeBlock renderEqualWidthForm>
            ui.equal.width.form {
                noui.fields {
                    noui.field {
                        label { +"Username" }
                        input(type = InputType.text) { placeholder = "Username" }
                    }
                    noui.field {
                        label { +"Password" }
                        input(type = InputType.password) { placeholder = "Password" }
                    }
                }
                noui.fields {
                    noui.field {
                        label { +"First Name" }
                        input(type = InputType.text) { placeholder = "First Name" }
                    }
                    noui.field {
                        label { +"Middle Name" }
                        input(type = InputType.text) { placeholder = "Middle Name" }
                    }
                    noui.field {
                        label { +"Last Name" }
                        input(type = InputType.text) { placeholder = "Last Name" }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderInverted() = example {
        ui.header H3 { +"Inverted" }

        p { +"A form on a dark background may have to invert its color scheme" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_form_FormPage_kt_renderInverted,
        ) {
            // <CodeBlock renderInverted>
            ui.inverted.segment {
                ui.inverted.form {
                    ui.two.fields {
                        noui.field {
                            label { +"First Name" }
                            input(type = InputType.text) { placeholder = "First Name" }
                        }
                        noui.field {
                            label { +"Last Name" }
                            input(type = InputType.text) { placeholder = "Last Name" }
                        }
                    }
                    noui.field {
                        ui.checkbox {
                            input { type = InputType.checkBox }
                            label { +"I agree to the Terms and Conditions" }
                        }
                    }
                    ui.button { +"Submit" }
                }
            }
            // </CodeBlock>
        }
    }

    // endregion

    // region Field Variations

    private fun FlowContent.renderInlineField() = example {
        ui.header H3 { +"Inline Field" }

        p { +"A field can have its label next to instead of above it" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_form_FormPage_kt_renderInlineField,
        ) {
            // <CodeBlock renderInlineField>
            ui.form {
                noui.inline.field {
                    label { +"Last Name" }
                    input(type = InputType.text) { placeholder = "Full Name" }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderFieldWidth() = example {
        ui.header H3 { +"Width" }

        p { +"A field can specify its width in grid columns" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_form_FormPage_kt_renderFieldWidth,
        ) {
            // <CodeBlock renderFieldWidth>
            ui.form {
                noui.fields {
                    noui.six.wide.field {
                        label { +"First Name" }
                        input(type = InputType.text) { placeholder = "First Name" }
                    }
                    noui.four.wide.field {
                        label { +"Middle" }
                        input(type = InputType.text) { placeholder = "Middle Name" }
                    }
                    noui.six.wide.field {
                        label { +"Last Name" }
                        input(type = InputType.text) { placeholder = "Last Name" }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderRequiredField() = example {
        ui.header H3 { +"Required" }

        p { +"A field can show that input is mandatory" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_form_FormPage_kt_renderRequiredField,
        ) {
            // <CodeBlock renderRequiredField>
            ui.form {
                noui.required.field {
                    label { +"Last Name" }
                    input(type = InputType.text) { placeholder = "Full Name" }
                }
                noui.required.field {
                    ui.checkbox {
                        input { type = InputType.checkBox }
                        label { +"I agree to the Terms and Conditions" }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    // endregion

    // region Group Variations

    private fun FlowContent.renderEqualWidthFields() = example {
        ui.header H3 { +"Equal Width Fields" }

        p { +"Fields can automatically divide fields to be equal width" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_form_FormPage_kt_renderEqualWidthFields,
        ) {
            // <CodeBlock renderEqualWidthFields>
            ui.form {
                ui.equal.width.fields {
                    noui.field {
                        label { +"Username" }
                        input(type = InputType.text) { placeholder = "Username" }
                    }
                    noui.field {
                        label { +"Password" }
                        input(type = InputType.password) { placeholder = "Password" }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderGroupedFields() = example {
        ui.header H3 { +"Grouped Fields" }

        p { +"Fields can show related choices" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_form_FormPage_kt_renderGroupedFields,
        ) {
            // <CodeBlock renderGroupedFields>
            ui.form {
                ui.grouped.fields {
                    label { +"Favorite Fruit" }
                    noui.field {
                        ui.checkbox {
                            input { type = InputType.checkBox; name = "fruit" }
                            label { +"Apples" }
                        }
                    }
                    noui.field {
                        ui.checkbox {
                            input { type = InputType.checkBox; name = "fruit" }
                            label { +"Oranges" }
                        }
                    }
                    noui.field {
                        ui.checkbox {
                            input { type = InputType.checkBox; name = "fruit" }
                            label { +"Pears" }
                        }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderInlineFields() = example {
        ui.header H3 { +"Inline Fields" }

        p { +"Multiple fields may be inline in a row" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_collections_form_FormPage_kt_renderInlineFields,
        ) {
            // <CodeBlock renderInlineFields>
            ui.form {
                ui.inline.fields {
                    label { +"Phone Number" }
                    noui.field {
                        input(type = InputType.text) { placeholder = "(xxx)" }
                    }
                    noui.field {
                        input(type = InputType.text) { placeholder = "xxx" }
                    }
                    noui.field {
                        input(type = InputType.text) { placeholder = "xxxx" }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    // endregion
}
