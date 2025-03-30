@file:Suppress(
    "DuplicatedCode",
    "Detekt:TooManyFunctions",
    "Detekt:LargeClass",
    "Detekt:LongMethod",
)

package de.peekandpoke.kraft.examples.fomanticui.pages.elements.icon

import de.peekandpoke.kraft.components.NoProps
import de.peekandpoke.kraft.components.PureComponent
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.examples.fomanticui.helpers.VerticalContentAndCode
import de.peekandpoke.kraft.examples.fomanticui.helpers.example
import de.peekandpoke.kraft.semanticui.icon
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.vdom.VDom
import generated.ExtractedCodeBlocks
import kotlinx.html.FlowContent
import kotlinx.html.Tag
import kotlinx.html.div

@Suppress("FunctionName")
fun Tag.IconExamples() = comp {
    IconExamples(it)
}

class IconExamples(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        renderSizes()
        renderColors()
        renderFeatures()
        renderIconGroupsAndCorners()
    }

    private fun FlowContent.renderSizes() = example {
        ui.dividing.header { +"Sizes" }

        VerticalContentAndCode(
            ExtractedCodeBlocks.pages_elements_icon_IconExamples_kt_renderSizes,
        ) {
            ui.doubling.eight.column.middle.aligned.grid {
                // <CodeBlock renderSizes>
                ui.center.aligned.column {
                    icon.mini.chess()
                    div { +"mini" }
                }
                ui.center.aligned.column {
                    icon.tiny.chess()
                    div { +"tiny" }
                }
                ui.center.aligned.column {
                    icon.small.chess()
                    div { +"small" }
                }
                ui.center.aligned.column {
                    icon.medium.chess()
                    div { +"medium" }
                }
                ui.center.aligned.column {
                    icon.large.chess()
                    div { +"large" }
                }
                ui.center.aligned.column {
                    icon.big.chess()
                    div { +"big" }
                }
                ui.center.aligned.column {
                    icon.huge.chess()
                    div { +"huge" }
                }
                ui.center.aligned.column {
                    icon.massive.chess()
                    div { +"massive" }
                }
                // </CodeBlock>
            }
        }
    }

    private fun FlowContent.renderColors() = example {
        ui.dividing.header { +"Colors" }

        VerticalContentAndCode(
            ExtractedCodeBlocks.pages_elements_icon_IconExamples_kt_renderColors,
        ) {
            ui.doubling.eight.column.grid {
                // <CodeBlock renderColors>
                ui.center.aligned.column {
                    icon.large.red.couch()
                    div { +"red" }
                }
                ui.center.aligned.column {
                    icon.large.orange.couch()
                    div { +"orange" }
                }
                ui.center.aligned.column {
                    icon.large.yellow.couch()
                    div { +"yellow" }
                }
                ui.center.aligned.column {
                    icon.large.olive.couch()
                    div { +"olive" }
                }
                ui.center.aligned.column {
                    icon.large.green.couch()
                    div { +"green" }
                }
                ui.center.aligned.column {
                    icon.large.teal.couch()
                    div { +"teal" }
                }
                ui.center.aligned.column {
                    icon.large.blue.couch()
                    div { +"blue" }
                }
                ui.center.aligned.column {
                    icon.large.violet.couch()
                    div { +"violet" }
                }
                ui.center.aligned.column {
                    icon.large.purple.couch()
                    div { +"purple" }
                }
                ui.center.aligned.column {
                    icon.large.pink.couch()
                    div { +"pink" }
                }
                ui.center.aligned.column {
                    icon.large.brown.couch()
                    div { +"brown" }
                }
                ui.center.aligned.column {
                    icon.large.grey.couch()
                    div { +"grey" }
                }
                ui.center.aligned.column {
                    icon.large.black.couch()
                    div { +"black" }
                }
                ui.center.aligned.column {
                    icon.large.white.couch()
                    div { +"white" }
                }
                ui.center.aligned.inverted.blue.column {
                    icon.large.white.couch()
                    div { +"white" }
                }
                // </CodeBlock>
            }
        }
    }

    private fun FlowContent.renderFeatures() = example {
        ui.dividing.header { +"Features" }

        VerticalContentAndCode(
            ExtractedCodeBlocks.pages_elements_icon_IconExamples_kt_renderFeatures,
        ) {
            ui.doubling.eight.column.grid {
                // <CodeBlock renderFeatures>
                ui.center.aligned.column {
                    icon.disabled.big.red.couch()
                    div { +"disabled" }
                }
                ui.center.aligned.column {
                    icon.big.loading.spinner()
                    icon.big.loading.circle_notch()
                    icon.big.loading.asterisk()
                    icon.big.loading.user()
                    div { +"loading" }
                }
                ui.center.aligned.column {
                    +"Fitted"
                    icon.fitted.globe()
                    +"icon"
                }
                ui.center.aligned.column {
                    icon.big.blue.link.question()
                    div { +"link" }
                }
                ui.center.aligned.column {
                    icon.big.horizontally.flipped.question()
                    icon.big.vertically.flipped.question()
                    div { +"flipped" }
                }
                ui.center.aligned.column {
                    icon.big.clockwise.rotated.question()
                    icon.big.counterclockwise.rotated.question()
                    div { +"rotated" }
                }
                ui.center.aligned.column {
                    icon.circular.users()
                    icon.circular.teal.users()
                    icon.circular.inverted.users()
                    icon.circular.inverted.teal.users()
                    div { +"circular" }
                }
                ui.center.aligned.column {
                    icon.circular.colored.red.users()
                    icon.circular.colored.green.users()
                    icon.circular.colored.blue.users()
                    div { +"colored circular" }
                }
                ui.center.aligned.column {
                    icon.bordered.users()
                    icon.bordered.teal.users()
                    icon.bordered.inverted.users()
                    icon.bordered.inverted.teal.users()
                    div { +"bordered" }
                }
                ui.center.aligned.column {
                    icon.bordered.colored.red.users()
                    icon.bordered.colored.green.users()
                    icon.bordered.colored.blue.users()
                    div { +"colored bordered" }
                }
                // </CodeBlock>
            }
        }
    }

    private fun FlowContent.renderIconGroupsAndCorners() = example {
        ui.dividing.header { +"Grouped icons & cornered icons" }

        VerticalContentAndCode(
            ExtractedCodeBlocks.pages_elements_icon_IconExamples_kt_renderIconGroupsAndCorners,
        ) {
            ui.doubling.eight.column.grid {
                // <CodeBlock renderIconGroupsAndCorners>
                ui.center.aligned.column {
                    ui.big.icons I {
                        icon.big.green.circle_outline()
                        icon.user()
                    }
                }
                ui.center.aligned.column {
                    ui.big.icons I {
                        icon.big.red.dont.render()
                        icon.user()
                    }
                }
                ui.center.aligned.column {
                    ui.big.icons I {
                        icon.user()
                        icon.top.left.corner.plus()
                    }
                }
                ui.center.aligned.column {
                    ui.big.icons I {
                        icon.user()
                        icon.top.right.corner.plus()
                    }
                }
                ui.center.aligned.column {
                    ui.big.icons I {
                        icon.user()
                        icon.bottom.right.corner.plus()
                    }
                }
                ui.center.aligned.column {
                    ui.big.icons I {
                        icon.user()
                        icon.bottom.left.corner.plus()
                    }
                }
                ui.center.aligned.column {
                    ui.big.icons I {
                        icon.twitter()
                        icon.bottom.right.corner.inverted.plus()
                    }
                }
                // </CodeBlock>
            }
        }
    }
}
