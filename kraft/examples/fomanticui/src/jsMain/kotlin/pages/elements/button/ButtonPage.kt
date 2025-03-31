@file:Suppress(
    "DuplicatedCode",
    "Detekt:TooManyFunctions",
    "Detekt:LargeClass",
    "Detekt:LongMethod",
)

package de.peekandpoke.kraft.examples.fomanticui.pages.elements.button

import de.peekandpoke.kraft.addons.routing.PageTitle
import de.peekandpoke.kraft.components.NoProps
import de.peekandpoke.kraft.components.PureComponent
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.components.onClick
import de.peekandpoke.kraft.examples.fomanticui.helpers.VerticalContentAndCode
import de.peekandpoke.kraft.examples.fomanticui.helpers.example
import de.peekandpoke.kraft.examples.fomanticui.helpers.readTheFomanticUiDocs
import de.peekandpoke.kraft.semanticui.icon
import de.peekandpoke.kraft.semanticui.noui
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.vdom.VDom
import generated.ExtractedCodeBlocks
import kotlinx.html.FlowContent
import kotlinx.html.Tag
import kotlinx.html.a
import kotlinx.html.div

@Suppress("FunctionName")
fun Tag.ButtonPage() = comp {
    ButtonPage(it)
}

class ButtonPage(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private var buttonState: Boolean by value(false)

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        PageTitle("Elements | Button")

        ui.basic.segment {
            ui.dividing.header H1 { +"Button" }

            readTheFomanticUiDocs("https://fomantic-ui.com/elements/button.html#/definition")

            ui.dividing.header H2 { +"Types" }

            renderStandardButton()
            renderEmphasizedButton()
            renderTertiaryButton()
            renderBasicButton()
            renderStatesOfButton()
            renderAnimatedButton()
            renderIconButton()
            renderLabeledButton()
            renderInvertedButton()

            ui.dividing.header H2 { +"Groups" }

            renderButtonGroup()

            ui.dividing.header H2 { +"Variations" }

            renderSocialButtons()
            renderSizedButtons()
            renderFloatedButtons()
            renderFluidButtons()
            renderCompactButtons()
            renderCircularButtons()

            ui.dividing.header H2 { +"... more ..." }

            a(href = "https://fomantic-ui.com/elements/button.html#vertical-buttons") {
                +"See more"
            }
        }
    }

    private fun FlowContent.renderStandardButton() = example {
        ui.header { +"A standard Button" }

        VerticalContentAndCode(
            ExtractedCodeBlocks.pages_elements_button_ButtonPage_kt_standardButton_1,
        ) {
            // <CodeBlock standardButton_1>
            ui.button {
                onClick { buttonState = !buttonState }
                +if (buttonState) "Following" else "Follow"
            }
            // </CodeBlock>
        }

        VerticalContentAndCode(
            ExtractedCodeBlocks.pages_elements_button_ButtonPage_kt_standardButton_2,
        ) {
            // <CodeBlock standardButton_2>
            ui.red.button { +"red" }
            ui.orange.button { +"orange" }
            ui.yellow.button { +"yellow" }
            ui.olive.button { +"olive" }
            ui.green.button { +"green" }
            ui.teal.button { +"teal" }
            ui.blue.button { +"blue" }
            ui.violet.button { +"violet" }
            ui.purple.button { +"purple" }
            ui.pink.button { +"pink" }
            ui.brown.button { +"brown" }
            ui.black.button { +"black" }
            ui.white.button { +"white" }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderEmphasizedButton() = example {
        ui.header { +"Emphasized Button" }

        VerticalContentAndCode(
            ExtractedCodeBlocks.pages_elements_button_ButtonPage_kt_renderEmphasizedButton,
        ) {
            // <CodeBlock renderEmphasizedButton>
            ui.primary.button { +"Primary" }
            ui.secondary.button { +"Secondary" }
            ui.positive.button { +"Positive" }
            ui.warning.button { +"Warning" }
            ui.negative.button { +"Negative" }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderTertiaryButton() = example {
        ui.header { +"Tertiary Button" }

        VerticalContentAndCode(
            ExtractedCodeBlocks.pages_elements_button_ButtonPage_kt_renderTertiaryButton_1,
        ) {
            // <CodeBlock renderTertiaryButton_1>
            ui.tertiary.button { +"Tertiary" }
            // </CodeBlock>
        }

        VerticalContentAndCode(
            ExtractedCodeBlocks.pages_elements_button_ButtonPage_kt_renderTertiaryButton_2,
        ) {
            // <CodeBlock renderTertiaryButton_2>
            ui.red.tertiary.button { +"red" }
            ui.orange.tertiary.button { +"orange" }
            ui.yellow.tertiary.button { +"yellow" }
            ui.olive.tertiary.button { +"olive" }
            ui.green.tertiary.button { +"green" }
            ui.teal.tertiary.button { +"teal" }
            ui.blue.tertiary.button { +"blue" }
            ui.violet.tertiary.button { +"violet" }
            ui.purple.tertiary.button { +"purple" }
            ui.pink.tertiary.button { +"pink" }
            ui.brown.tertiary.button { +"brown" }
            ui.black.tertiary.button { +"black" }
            ui.white.tertiary.button { +"white" }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderBasicButton() = example {
        ui.header { +"Basic Button" }

        ui.stackable.three.column.grid {
            ui.column {
                VerticalContentAndCode(
                    ExtractedCodeBlocks.pages_elements_button_ButtonPage_kt_renderBasicButton_1,
                ) {
                    // <CodeBlock renderBasicButton_1>
                    ui.basic.icon.button {
                        icon.clock()
                    }
                    // </CodeBlock>
                }
            }
            ui.column {
                VerticalContentAndCode(
                    ExtractedCodeBlocks.pages_elements_button_ButtonPage_kt_renderBasicButton_2,
                ) {
                    // <CodeBlock renderBasicButton_2>
                    ui.basic.button {
                        icon.mouse()
                        +"Click me"
                    }
                    // </CodeBlock>
                }
            }
            ui.column {
                VerticalContentAndCode(
                    ExtractedCodeBlocks.pages_elements_button_ButtonPage_kt_renderBasicButton_3,
                ) {
                    // <CodeBlock renderBasicButton_3>
                    ui.basic.blue.button {
                        icon.mouse_pointer()
                        +"Click me"
                    }
                    // </CodeBlock>
                }
            }
        }

        VerticalContentAndCode(
            ExtractedCodeBlocks.pages_elements_button_ButtonPage_kt_renderBasicButton_4,
        ) {
            // <CodeBlock renderBasicButton_4>
            ui.red.basic.button { +"red" }
            ui.orange.basic.button { +"orange" }
            ui.yellow.basic.button { +"yellow" }
            ui.olive.basic.button { +"olive" }
            ui.green.basic.button { +"green" }
            ui.teal.basic.button { +"teal" }
            ui.blue.basic.button { +"blue" }
            ui.violet.basic.button { +"violet" }
            ui.purple.basic.button { +"purple" }
            ui.pink.basic.button { +"pink" }
            ui.brown.basic.button { +"brown" }
            ui.black.basic.button { +"black" }
            ui.white.basic.button { +"white" }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderStatesOfButton() = example {
        ui.header { +"Active and Disabled states" }

        VerticalContentAndCode(
            ExtractedCodeBlocks.pages_elements_button_ButtonPage_kt_renderStatesOfButton_1,
        ) {
            // <CodeBlock renderStatesOfButton_1>
            ui.active.button { +"Active" }
            ui.active.primary.button { +"Primary" }
            ui.active.secondary.button { +"Secondary" }
            // </CodeBlock>
        }

        VerticalContentAndCode(
            ExtractedCodeBlocks.pages_elements_button_ButtonPage_kt_renderStatesOfButton_2,
        ) {
            // <CodeBlock renderStatesOfButton_2>
            ui.disabled.button { +"Disabled" }
            ui.disabled.primary.button { +"Primary" }
            ui.disabled.secondary.button { +"Secondary" }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderAnimatedButton() = example {
        ui.header { +"Animated Button" }

        ui.stackable.three.column.grid {
            ui.column {
                VerticalContentAndCode(
                    ExtractedCodeBlocks.pages_elements_button_ButtonPage_kt_renderAnimatedButton_1,
                ) {
                    // <CodeBlock renderAnimatedButton_1>
                    ui.animated.button {
                        noui.visible.content { +"Next" }
                        noui.hidden.content {
                            icon.arrow_right()
                        }
                    }
                    // </CodeBlock>
                }
            }

            ui.column {
                VerticalContentAndCode(
                    ExtractedCodeBlocks.pages_elements_button_ButtonPage_kt_renderAnimatedButton_2,
                ) {
                    // <CodeBlock renderAnimatedButton_2>
                    ui.vertical.animated.button {
                        noui.hidden.content { +"Shop" }
                        noui.visible.content {
                            icon.shopping_cart()
                        }
                    }
                    // </CodeBlock>
                }
            }

            ui.column {
                VerticalContentAndCode(
                    ExtractedCodeBlocks.pages_elements_button_ButtonPage_kt_renderAnimatedButton_3,
                ) {
                    // <CodeBlock renderAnimatedButton_3>
                    ui.animated.fade.button {
                        noui.visible.content { +"Sign-up for a Pro account" }
                        noui.hidden.content { +"\$12.99 a month" }
                    }
                    // </CodeBlock>
                }
            }
        }
    }

    private fun FlowContent.renderIconButton() = example {
        ui.header { +"Icon Button" }

        ui.stackable.three.column.grid {
            ui.column {
                VerticalContentAndCode(
                    ExtractedCodeBlocks.pages_elements_button_ButtonPage_kt_renderIconButton_1,
                ) {
                    // <CodeBlock renderIconButton_1>
                    ui.icon.button {
                        icon.clock()
                    }
                    // </CodeBlock>
                }
            }
            ui.column {
                VerticalContentAndCode(
                    ExtractedCodeBlocks.pages_elements_button_ButtonPage_kt_renderIconButton_2,
                ) {
                    // <CodeBlock renderIconButton_2>
                    ui.basic.red.labeled.icon.button {
                        icon.pause()
                        +"Pause"
                    }
                    // </CodeBlock>
                }
            }
            ui.column {
                VerticalContentAndCode(
                    ExtractedCodeBlocks.pages_elements_button_ButtonPage_kt_renderIconButton_3,
                ) {
                    // <CodeBlock renderIconButton_3>
                    ui.blue.right.labeled.icon.button {
                        icon.play()
                        +"Play"
                    }
                    // </CodeBlock>
                }
            }
        }
    }

    private fun FlowContent.renderLabeledButton() = example {
        ui.header { +"Labelled Button" }

        ui.stackable.three.column.grid {
            ui.column {
                VerticalContentAndCode(
                    ExtractedCodeBlocks.pages_elements_button_ButtonPage_kt_renderLabeledButton_1,
                ) {
                    // <CodeBlock renderLabeledButton_1>
                    ui.labeled.button {
                        ui.button {
                            icon.heart(); +"Like"
                        }
                        ui.basic.label A {
                            +"2,048"
                        }
                    }
                    // </CodeBlock>
                }
            }

            ui.column {
                VerticalContentAndCode(
                    ExtractedCodeBlocks.pages_elements_button_ButtonPage_kt_renderLabeledButton_2,
                ) {
                    // <CodeBlock renderLabeledButton_2>
                    ui.left.labeled.button {
                        ui.basic.right.pointing.label A {
                            +"2,048"
                        }
                        ui.button {
                            icon.heart(); +"Like"
                        }
                    }
                    // </CodeBlock>
                }
            }

            ui.column {
                VerticalContentAndCode(
                    ExtractedCodeBlocks.pages_elements_button_ButtonPage_kt_renderLabeledButton_3,
                ) {
                    // <CodeBlock renderLabeledButton_3>
                    ui.left.labeled.button {
                        ui.basic.label A {
                            +"2,048"
                        }
                        ui.icon.button {
                            icon.code_branch()
                        }
                    }
                    // </CodeBlock>
                }
            }

            ui.column {
                VerticalContentAndCode(
                    ExtractedCodeBlocks.pages_elements_button_ButtonPage_kt_renderLabeledButton_4,
                ) {
                    // <CodeBlock renderLabeledButton_4>
                    ui.labeled.button {
                        ui.red.button {
                            icon.heart(); +"Like"
                        }
                        ui.red.basic.label A {
                            +"2,048"
                        }
                    }
                    // </CodeBlock>
                }
            }

            ui.column {
                VerticalContentAndCode(
                    ExtractedCodeBlocks.pages_elements_button_ButtonPage_kt_renderLabeledButton_5,
                ) {
                    // <CodeBlock renderLabeledButton_5>
                    ui.labeled.button {
                        ui.basic.blue.button {
                            icon.code_branch(); +"Forks"
                        }
                        ui.basic.left.pointing.blue.label A {
                            +"2,048"
                        }
                    }
                    // </CodeBlock>
                }
            }
        }
    }

    private fun FlowContent.renderInvertedButton() = example {
        ui.header { +"Inverted Button" }

        VerticalContentAndCode(
            ExtractedCodeBlocks.pages_elements_button_ButtonPage_kt_renderInvertedButton_1,
        ) {
            ui.inverted.segment {
                // <CodeBlock renderInvertedButton_1>
                ui.inverted.button { +"standard" }
                ui.inverted.primary.button { +"primary" }
                ui.inverted.secondary.button { +"secondary" }
                ui.inverted.positive.button { +"positive" }
                ui.inverted.negative.button { +"negative" }
                ui.inverted.red.button { +"red" }
                ui.inverted.orange.button { +"orange" }
                ui.inverted.yellow.button { +"yellow" }
                ui.inverted.olive.button { +"olive" }
                ui.inverted.green.button { +"green" }
                ui.inverted.teal.button { +"teal" }
                ui.inverted.blue.button { +"blue" }
                ui.inverted.violet.button { +"violet" }
                ui.inverted.purple.button { +"purple" }
                ui.inverted.pink.button { +"pink" }
                ui.inverted.brown.button { +"brown" }
                ui.inverted.black.button { +"black" }
                ui.inverted.white.button { +"white" }
                // </CodeBlock>
            }
        }

        VerticalContentAndCode(
            ExtractedCodeBlocks.pages_elements_button_ButtonPage_kt_renderInvertedButton_2,
        ) {
            ui.inverted.segment {
                // <CodeBlock renderInvertedButton_2>
                ui.inverted.basic.button { +"standard" }
                ui.inverted.basic.primary.button { +"primary" }
                ui.inverted.basic.secondary.button { +"secondary" }
                ui.inverted.basic.positive.button { +"positive" }
                ui.inverted.basic.negative.button { +"negative" }
                ui.inverted.basic.red.button { +"red" }
                ui.inverted.basic.orange.button { +"orange" }
                ui.inverted.basic.yellow.button { +"yellow" }
                ui.inverted.basic.olive.button { +"olive" }
                ui.inverted.basic.green.button { +"green" }
                ui.inverted.basic.teal.button { +"teal" }
                ui.inverted.basic.blue.button { +"blue" }
                ui.inverted.basic.violet.button { +"violet" }
                ui.inverted.basic.purple.button { +"purple" }
                ui.inverted.basic.pink.button { +"pink" }
                ui.inverted.basic.brown.button { +"brown" }
                ui.inverted.basic.black.button { +"black" }
                ui.inverted.basic.white.button { +"white" }
                // </CodeBlock>
            }
        }
    }

    private fun FlowContent.renderButtonGroup() = example {
        ui.header { +"Button Group" }

        VerticalContentAndCode(
            ExtractedCodeBlocks.pages_elements_button_ButtonPage_kt_renderButtonGroup_1,
        ) {
            // <CodeBlock renderButtonGroup_1>
            ui.buttons {

                ui.button { +"One" }
                ui.button { +"Two" }
                ui.button { +"Three" }
            }
            // </CodeBlock>
        }

        VerticalContentAndCode(
            ExtractedCodeBlocks.pages_elements_button_ButtonPage_kt_renderButtonGroup_2,
        ) {
            // <CodeBlock renderButtonGroup_2>
            ui.icon.buttons {
                ui.button { icon.align_left() }
                ui.button { icon.align_center() }
                ui.button { icon.align_justify() }
            }
            ui.icon.buttons {
                ui.button { icon.bold() }
                ui.button { icon.underline() }
                ui.button { icon.text_width() }
            }
            // </CodeBlock>
        }

        VerticalContentAndCode(
            ExtractedCodeBlocks.pages_elements_button_ButtonPage_kt_renderButtonGroup_3,
        ) {
            // <CodeBlock renderButtonGroup_3>
            ui.buttons {
                ui.button { +"Cancel" }
                div("or") {}
                ui.positive.button { +"Save" }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderSocialButtons() = example {
        ui.header { +"Social Buttons" }

        VerticalContentAndCode(
            ExtractedCodeBlocks.pages_elements_button_ButtonPage_kt_renderSocialButtons,
        ) {
            // <CodeBlock renderSocialButtons>
            // Facebook
            ui.facebook.button { icon.facebook(); +"Facebook" }
            ui.facebook.icon.button { icon.facebook_f() }
            // Google+
            ui.google_plus.button { icon.google_plus(); +"Google Plus" }
            ui.google_plus.icon.button { icon.google_plus() }
            // Instagram
            ui.instagram.button { icon.instagram(); +"Instagram" }
            ui.instagram.icon.button { icon.instagram() }
            // Linkedin
            ui.linkedin.button { icon.linkedin(); +"Linkedin" }
            ui.linkedin.icon.button { icon.linkedin() }
            // Telegram
            ui.telegram.button { icon.telegram(); +"Telegram" }
            ui.telegram.icon.button { icon.telegram() }
            // Twitter
            ui.twitter.button { icon.twitter(); +"Twitter" }
            ui.twitter.icon.button { icon.twitter() }
            // WhatsApp
            ui.whatsapp.button { icon.whatsapp(); +"WhatsApp" }
            ui.whatsapp.icon.button { icon.whatsapp() }
            // VK
            ui.vk.button { icon.vk(); +"VK" }
            ui.vk.icon.button { icon.vk() }
            // Youtube
            ui.youtube.button { icon.youtube(); +"Youtube" }
            ui.youtube.icon.button { icon.youtube() }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderSizedButtons() = example {
        ui.header { +"Sized Buttons" }

        VerticalContentAndCode(
            ExtractedCodeBlocks.pages_elements_button_ButtonPage_kt_renderSizedButtons,
        ) {
            // <CodeBlock renderSizedButtons>
            ui.mini.button { +"mini" }
            ui.tiny.button { +"tiny" }
            ui.small.button { +"small" }
            ui.medium.button { +"medium" }
            ui.large.button { +"large" }
            ui.big.button { +"big" }
            ui.huge.button { +"huge" }
            ui.massive.button { +"massive" }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderFloatedButtons() = example {
        ui.header { +"Floated Buttons" }

        VerticalContentAndCode(
            ExtractedCodeBlocks.pages_elements_button_ButtonPage_kt_renderFloatedButtons,
        ) {
            // <CodeBlock renderFloatedButtons>
            ui.left.floated.button { +"Left floated" }
            ui.right.floated.button { +"Right floated" }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderFluidButtons() = example {
        ui.header { +"Fluid Buttons" }

        VerticalContentAndCode(
            ExtractedCodeBlocks.pages_elements_button_ButtonPage_kt_renderFluidButtons,
        ) {
            // <CodeBlock renderFluidButtons>
            ui.fluid.button { +"Fluid" }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderCompactButtons() = example {
        ui.header { +"Compact Buttons" }

        VerticalContentAndCode(
            ExtractedCodeBlocks.pages_elements_button_ButtonPage_kt_renderCompactButtons,
        ) {
            // <CodeBlock renderCompactButtons>
            ui.compact.button {
                +"Compact"
            }
            ui.compact.icon.button {
                icon.pause()
            }
            ui.compact.labeled.icon.button {
                icon.pause()
                +"Pause"
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderCircularButtons() = example {
        ui.header { +"Circular Buttons" }

        VerticalContentAndCode(
            ExtractedCodeBlocks.pages_elements_button_ButtonPage_kt_renderCircularButtons,
        ) {
            // <CodeBlock renderCircularButtons>
            ui.circular.icon.button {
                icon.cogs()
            }
            ui.circular.facebook.icon.button {
                icon.facebook_f()
            }
            ui.circular.green.icon.button {
                icon.envelope()
            }
            // </CodeBlock>
        }
    }
}
