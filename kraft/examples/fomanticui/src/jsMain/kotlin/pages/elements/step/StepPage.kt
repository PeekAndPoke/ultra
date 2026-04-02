@file:Suppress(
    "DuplicatedCode",
    "Detekt:TooManyFunctions",
    "Detekt:LargeClass",
    "Detekt:LongMethod",
)

package io.peekandpoke.kraft.examples.fomanticui.pages.elements.step

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
import io.peekandpoke.ultra.semanticui.noui
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.html.FlowContent
import kotlinx.html.Tag
import kotlinx.html.p

@Suppress("FunctionName")
fun Tag.StepPage() = comp {
    StepPage(it)
}

class StepPage(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        PageTitle("Elements | Step")

        ui.basic.segment {
            ui.dividing.header H1 { +"Step" }

            readTheFomanticUiDocs("https://fomantic-ui.com/elements/step.html")

            ui.dividing.header H2 { +"Types" }

            renderStep()
            renderSteps()
            renderOrdered()
            renderVertical()

            ui.dividing.header H2 { +"Content" }

            renderDescription()
            renderIcon()
            renderLinkStep()

            ui.dividing.header H2 { +"States" }

            renderActive()
            renderCompleted()
            renderDisabled()

            ui.dividing.header H2 { +"Variations" }

            renderStackable()
            renderFluid()
            renderAttached()
            renderEvenlyDivided()
            renderSize()
            renderInverted()
        }
    }

    private fun FlowContent.renderStep() = example {
        ui.header H3 { +"Step" }

        p { +"A single step." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_step_StepPage_kt_renderStep,
        ) {
            // <CodeBlock renderStep>
            ui.steps {
                noui.step {
                    +"Shipping"
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderSteps() = example {
        ui.header H3 { +"Steps" }

        p { +"A set of steps." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_step_StepPage_kt_renderSteps,
        ) {
            // <CodeBlock renderSteps>
            ui.steps {
                noui.completed.step {
                    icon.truck()
                    noui.content {
                        noui.title { +"Shipping" }
                        noui.description { +"Choose your shipping options" }
                    }
                }
                noui.active.step {
                    icon.credit_card()
                    noui.content {
                        noui.title { +"Billing" }
                        noui.description { +"Enter billing information" }
                    }
                }
                noui.disabled.step {
                    icon.info_circle()
                    noui.content {
                        noui.title { +"Confirm Order" }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderOrdered() = example {
        ui.header H3 { +"Ordered" }

        p { +"A step can show a ordered sequence of steps." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_step_StepPage_kt_renderOrdered,
        ) {
            // <CodeBlock renderOrdered>
            ui.ordered.steps {
                noui.completed.step {
                    noui.content {
                        noui.title { +"Shipping" }
                        noui.description { +"Choose your shipping options" }
                    }
                }
                noui.completed.step {
                    noui.content {
                        noui.title { +"Billing" }
                        noui.description { +"Enter billing information" }
                    }
                }
                noui.active.step {
                    noui.content {
                        noui.title { +"Confirm Order" }
                        noui.description { +"Verify order details" }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderVertical() = example {
        ui.header H3 { +"Vertical" }

        p { +"A step can be displayed stacked vertically." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_step_StepPage_kt_renderVertical,
        ) {
            // <CodeBlock renderVertical>
            ui.vertical.steps {
                noui.completed.step {
                    icon.truck()
                    noui.content {
                        noui.title { +"Shipping" }
                        noui.description { +"Choose your shipping options" }
                    }
                }
                noui.active.step {
                    icon.credit_card()
                    noui.content {
                        noui.title { +"Billing" }
                        noui.description { +"Enter billing information" }
                    }
                }
                noui.disabled.step {
                    icon.info_circle()
                    noui.content {
                        noui.title { +"Confirm Order" }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderDescription() = example {
        ui.header H3 { +"Description" }

        p { +"A step can contain a description." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_step_StepPage_kt_renderDescription,
        ) {
            // <CodeBlock renderDescription>
            ui.steps {
                noui.step {
                    noui.content {
                        noui.title { +"Shipping" }
                        noui.description { +"Choose your shipping options" }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderIcon() = example {
        ui.header H3 { +"Icon" }

        p { +"A step can contain an icon." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_step_StepPage_kt_renderIcon,
        ) {
            // <CodeBlock renderIcon>
            ui.steps {
                noui.step {
                    icon.truck()
                    noui.content {
                        noui.title { +"Shipping" }
                        noui.description { +"Choose your shipping options" }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderLinkStep() = example {
        ui.header H3 { +"Link" }

        p { +"A step can link." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_step_StepPage_kt_renderLinkStep,
        ) {
            // <CodeBlock renderLinkStep>
            ui.steps {
                noui.active.step A {
                    icon.truck()
                    noui.content {
                        noui.title { +"Shipping" }
                        noui.description { +"Choose your shipping options" }
                    }
                }
                noui.step A {
                    noui.content {
                        noui.title { +"Billing" }
                        noui.description { +"Enter billing information" }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderActive() = example {
        ui.header H3 { +"Active" }

        p { +"A step can be highlighted as active." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_step_StepPage_kt_renderActive,
        ) {
            // <CodeBlock renderActive>
            ui.steps {
                noui.active.step {
                    icon.credit_card()
                    noui.content {
                        noui.title { +"Billing" }
                        noui.description { +"Enter billing information" }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderCompleted() = example {
        ui.header H3 { +"Completed" }

        p { +"A step can show that a user has completed it." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_step_StepPage_kt_renderCompleted,
        ) {
            // <CodeBlock renderCompleted>
            ui.steps {
                noui.completed.step {
                    icon.credit_card()
                    noui.content {
                        noui.title { +"Billing" }
                        noui.description { +"Enter billing information" }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderDisabled() = example {
        ui.header H3 { +"Disabled" }

        p { +"A step can show that it cannot be selected." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_step_StepPage_kt_renderDisabled,
        ) {
            // <CodeBlock renderDisabled>
            ui.steps {
                noui.disabled.step {
                    noui.content {
                        noui.title { +"Billing" }
                        noui.description { +"Enter billing information" }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderStackable() = example {
        ui.header H3 { +"Stackable" }

        p { +"A step can stack vertically only on smaller screens." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_step_StepPage_kt_renderStackable,
        ) {
            // <CodeBlock renderStackable>
            ui.tablet.stackable.steps {
                noui.step {
                    icon.truck()
                    noui.content {
                        noui.title { +"Shipping" }
                        noui.description { +"Choose your shipping options" }
                    }
                }
                noui.active.step {
                    icon.credit_card()
                    noui.content {
                        noui.title { +"Billing" }
                        noui.description { +"Enter billing information" }
                    }
                }
                noui.disabled.step {
                    icon.info_circle()
                    noui.content {
                        noui.title { +"Confirm Order" }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderFluid() = example {
        ui.header H3 { +"Fluid" }

        p { +"A fluid step takes up the width of its container." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_step_StepPage_kt_renderFluid,
        ) {
            // <CodeBlock renderFluid>
            ui.two.fluid.steps {
                noui.active.step {
                    icon.truck()
                    noui.content {
                        noui.title { +"Shipping" }
                        noui.description { +"Choose your shipping options" }
                    }
                }
                noui.step {
                    icon.credit_card()
                    noui.content {
                        noui.title { +"Billing" }
                        noui.description { +"Enter billing information" }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderAttached() = example {
        ui.header H3 { +"Attached" }

        p { +"Steps can be attached to other elements." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_step_StepPage_kt_renderAttached,
        ) {
            // <CodeBlock renderAttached>
            ui.three.top.attached.steps {
                noui.step {
                    icon.truck()
                    noui.content {
                        noui.title { +"Shipping" }
                    }
                }
                noui.active.step {
                    icon.credit_card()
                    noui.content {
                        noui.title { +"Billing" }
                    }
                }
                noui.disabled.step {
                    icon.info_circle()
                    noui.content {
                        noui.title { +"Confirm" }
                    }
                }
            }
            ui.attached.segment {
                p { +"Segment content" }
            }
            ui.three.bottom.attached.steps {
                noui.step {
                    icon.truck()
                    noui.content {
                        noui.title { +"Shipping" }
                    }
                }
                noui.active.step {
                    icon.credit_card()
                    noui.content {
                        noui.title { +"Billing" }
                    }
                }
                noui.disabled.step {
                    icon.info_circle()
                    noui.content {
                        noui.title { +"Confirm" }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderEvenlyDivided() = example {
        ui.header H3 { +"Evenly Divided" }

        p { +"Steps can be divided evenly inside their parent." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_step_StepPage_kt_renderEvenlyDivided,
        ) {
            // <CodeBlock renderEvenlyDivided>
            ui.two.steps {
                noui.active.step {
                    icon.truck()
                    noui.content {
                        noui.title { +"Shipping" }
                    }
                }
                noui.disabled.step {
                    icon.info_circle()
                    noui.content {
                        noui.title { +"Confirm Order" }
                    }
                }
            }

            ui.three.steps {
                noui.active.step {
                    icon.truck()
                    noui.content {
                        noui.title { +"Shipping" }
                    }
                }
                noui.step {
                    icon.credit_card()
                    noui.content {
                        noui.title { +"Billing" }
                    }
                }
                noui.disabled.step {
                    icon.info_circle()
                    noui.content {
                        noui.title { +"Confirm Order" }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderSize() = example {
        ui.header H3 { +"Size" }

        p { +"Steps can have different sizes." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_step_StepPage_kt_renderSize,
        ) {
            // <CodeBlock renderSize>
            ui.mini.steps {
                noui.step {
                    icon.truck()
                    noui.content {
                        noui.title { +"Shipping" }
                    }
                }
                noui.active.step {
                    icon.credit_card()
                    noui.content {
                        noui.title { +"Billing" }
                    }
                }
                noui.disabled.step {
                    icon.info_circle()
                    noui.content {
                        noui.title { +"Confirm" }
                    }
                }
            }

            ui.small.steps {
                noui.step {
                    icon.truck()
                    noui.content {
                        noui.title { +"Shipping" }
                    }
                }
                noui.active.step {
                    icon.credit_card()
                    noui.content {
                        noui.title { +"Billing" }
                    }
                }
                noui.disabled.step {
                    icon.info_circle()
                    noui.content {
                        noui.title { +"Confirm" }
                    }
                }
            }

            ui.large.steps {
                noui.step {
                    icon.truck()
                    noui.content {
                        noui.title { +"Shipping" }
                    }
                }
                noui.active.step {
                    icon.credit_card()
                    noui.content {
                        noui.title { +"Billing" }
                    }
                }
                noui.disabled.step {
                    icon.info_circle()
                    noui.content {
                        noui.title { +"Confirm" }
                    }
                }
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.renderInverted() = example {
        ui.header H3 { +"Inverted" }

        p { +"A step's color can be inverted." }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_elements_step_StepPage_kt_renderInverted,
        ) {
            // <CodeBlock renderInverted>
            ui.inverted.segment {
                ui.inverted.steps {
                    noui.completed.step {
                        icon.truck()
                        noui.content {
                            noui.title { +"Shipping" }
                            noui.description { +"Choose your shipping options" }
                        }
                    }
                    noui.active.step {
                        icon.credit_card()
                        noui.content {
                            noui.title { +"Billing" }
                            noui.description { +"Enter billing information" }
                        }
                    }
                    noui.disabled.step {
                        icon.info_circle()
                        noui.content {
                            noui.title { +"Confirm Order" }
                        }
                    }
                }
            }
            // </CodeBlock>
        }
    }
}
