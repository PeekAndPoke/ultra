@file:Suppress(
    "DuplicatedCode",
    "Detekt:TooManyFunctions",
    "Detekt:LargeClass",
    "Detekt:LongMethod",
)

package io.peekandpoke.kraft.examples.fomanticui.pages.howto.kraft

import generated.ExtractedCodeBlocks
import io.peekandpoke.kraft.addons.prismjs.PrismKotlin
import io.peekandpoke.kraft.addons.prismjs.PrismPlugin.CopyToClipboard.Companion.copyToClipboard
import io.peekandpoke.kraft.addons.prismjs.PrismPlugin.LineNumbers.Companion.lineNumbers
import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.examples.fomanticui.helpers.HorizontalContentAndCode
import io.peekandpoke.kraft.examples.fomanticui.helpers.example
import io.peekandpoke.kraft.examples.fomanticui.pages.howto.kraft.code.FunctionalPropsComponent
import io.peekandpoke.kraft.examples.fomanticui.pages.howto.kraft.code.FunctionalPureComponent
import io.peekandpoke.kraft.examples.fomanticui.pages.howto.kraft.code.PropsExampleComponent
import io.peekandpoke.kraft.examples.fomanticui.pages.howto.kraft.code.PureExampleComponent
import io.peekandpoke.kraft.routing.PageTitle
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.html.FlowContent
import kotlinx.html.Tag
import kotlinx.html.p

@Suppress("FunctionName")
fun Tag.KraftComponentBasicsPage() = comp {
    KraftComponentsPage(it)
}

class KraftComponentsPage(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        PageTitle("Kraft | Component Basics")

        ui.basic.padded.segment {
            ui.dividing.header H1 { +"Component Basics" }

            pureComponentsExample()
            propsComponentsExample()
            functionalPureComponentsExample()
            functionalPropsComponentsExample()
        }
    }

    private fun FlowContent.pureComponentsExample() = example {

        ui.dividing.header H2 { +"Pure component" }

        p { +"A pure component does not get props passed to it." }
        p { +"A pure component is defined like follows:" }

        PrismKotlin(
            ExtractedCodeBlocks.pages_howto_kraft_code_PureExampleComponent_kt_code,
        ) {
            lineNumbers()
            copyToClipboard()
        }

        ui.divider()

        p { +"Once the component is defined it can be used like this:" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_howto_kraft_KraftComponentBasicsPage_kt_pureComponentsExample,
        ) {
            // <CodeBlock pureComponentsExample>
            ui.segment {
                PureExampleComponent()
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.propsComponentsExample() = example {

        ui.dividing.header H2 { +"Parameterized component with Props" }

        p { +"A component can accept parameters, so called Props." }
        p { +"A component with Props is defined like follows:" }

        PrismKotlin(
            ExtractedCodeBlocks.pages_howto_kraft_code_PropsExampleComponent_kt_code,
        ) {
            lineNumbers()
            copyToClipboard()
        }

        ui.divider()

        p { +"Once the component is defined it can be used like this:" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_howto_kraft_KraftComponentBasicsPage_kt_propsComponentsExample,
        ) {
            // <CodeBlock propsComponentsExample>
            ui.segment {
                PropsExampleComponent("KRAFT")
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.functionalPureComponentsExample() = example {

        ui.dividing.header H2 { +"Functional pure component" }

        p { +"A pure component can be defined in a functional way:" }

        PrismKotlin(
            ExtractedCodeBlocks.pages_howto_kraft_code_FunctionalPureComponent_kt_code,
        ) {
            lineNumbers()
            copyToClipboard()
        }

        ui.divider()

        p { +"Once the component is defined it can be used like this:" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_howto_kraft_KraftComponentBasicsPage_kt_functionalPureComponentsExample,
        ) {
            // <CodeBlock functionalPureComponentsExample>
            ui.segment {
                FunctionalPureComponent()
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.functionalPropsComponentsExample() = example {

        ui.dividing.header H2 { +"Functional component with Props" }

        p { +"A functional component can take up to 10 parameters." }
        p { +"A functional component with parameters is defined as follows:" }

        PrismKotlin(
            ExtractedCodeBlocks.pages_howto_kraft_code_FunctionalPropsComponent_kt_code,
        ) {
            lineNumbers()
            copyToClipboard()
        }

        ui.divider()

        p { +"Once the component is defined it can be used like this:" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_howto_kraft_KraftComponentBasicsPage_kt_functionalPropsComponentsExample,
        ) {
            // <CodeBlock functionalPropsComponentsExample>
            ui.segment {
                FunctionalPropsComponent(11, 22)
            }
            // </CodeBlock>
        }
    }
}
