@file:Suppress(
    "DuplicatedCode",
    "Detekt:TooManyFunctions",
    "Detekt:LargeClass",
    "Detekt:LongMethod",
)

package de.peekandpoke.kraft.examples.fomanticui.pages.howto.kraft

import de.peekandpoke.kraft.addons.prismjs.PrismKotlin
import de.peekandpoke.kraft.addons.prismjs.PrismPlugin.CopyToClipboard.Companion.copyToClipboard
import de.peekandpoke.kraft.addons.prismjs.PrismPlugin.LineNumbers.Companion.lineNumbers
import de.peekandpoke.kraft.addons.routing.PageTitle
import de.peekandpoke.kraft.components.NoProps
import de.peekandpoke.kraft.components.PureComponent
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.examples.fomanticui.helpers.HorizontalContentAndCode
import de.peekandpoke.kraft.examples.fomanticui.helpers.example
import de.peekandpoke.kraft.examples.fomanticui.pages.howto.kraft.code.CounterComponent
import de.peekandpoke.kraft.examples.fomanticui.pages.howto.kraft.code.FunctionalCounterComponent
import de.peekandpoke.kraft.examples.fomanticui.pages.howto.kraft.code.FunctionalTickerComponent
import de.peekandpoke.kraft.examples.fomanticui.pages.howto.kraft.code.TickerComponent
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.vdom.VDom
import generated.ExtractedCodeBlocks
import kotlinx.html.FlowContent
import kotlinx.html.Tag
import kotlinx.html.p

@Suppress("FunctionName")
fun Tag.KraftComponentStatePage() = comp {
    KraftComponentStatePage(it)
}

class KraftComponentStatePage(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        PageTitle("Kraft | Component State")

        ui.basic.padded.segment {
            ui.dividing.header H1 { +"Component State" }

            ui.dividing.header H2 { +"Introduction" }
            introduction()

            ui.dividing.header H2 { +"Examples for class components" }

            byValueExample()
            bySubscribingToExample()

            ui.dividing.header H2 { +"Examples for functional components" }

            functionalByValueExample()
            functionalBySubscribingToExample()
        }
    }

    private fun FlowContent.introduction() = example {

        p {
            +"From Preact or React you might know that there is a function called "
            ui.label { +"setState" }
            +" that has to be called when the state of a component is changed."
        }
        p {
            +"setState() has to be called so underlying framework knows that the component needs to be redrawn."
        }

        p {
            +"KRAFT uses the same concept but hides it from the user by leveraging "
            +"Kotlin's delegated properties. See the examples below"
        }
    }

    private fun FlowContent.byValueExample() = example {
        ui.dividing.header H3 { +"by value" }

        p { +"State can be stored by using:" }

        PrismKotlin(
            "var x by value(100)"
        )

        p {
            +"So whenever 'x' is changed the component will be redrawn. "
            +"A component can be have multiple properties defined like this."
        }

        p { +"Below you will find an example using this method:" }

        PrismKotlin(
            ExtractedCodeBlocks.pages_howto_kraft_code_CounterComponent_kt_code,
        ) {
            lineNumbers()
            copyToClipboard()
        }

        p { +"And here is the CounterComponent in action" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_howto_kraft_KraftComponentStatePage_kt_byValueExample,
        ) {
            // <CodeBlock byValueExample>
            ui.segment {
                CounterComponent(100)
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.bySubscribingToExample() = example {
        ui.dividing.header H3 { +"by subscribingTo" }

        p {
            +"Components can subscribe to streams. "
            +"Every time the stream publishes a new value the component will be redrawn."
        }

        p {
            +"When the component is removed from the DOM it will automatically unsubscrive from all streams."
        }

        p {
            +"Subscribing to stream can be done like this:"
        }

        PrismKotlin(
            "val x by subscribingTo(someStream)"
        )

        p { +"Below you will find an example using this method:" }

        PrismKotlin(
            ExtractedCodeBlocks.pages_howto_kraft_code_TickerComponent_kt_code,
        ) {
            lineNumbers()
            copyToClipboard()
        }

        p { +"And here is the TickerComponent in action" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_howto_kraft_KraftComponentStatePage_kt_bySubscribingToExample,
        ) {
            // <CodeBlock bySubscribingToExample>
            ui.segment {
                TickerComponent(100)
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.functionalByValueExample() = example {
        ui.dividing.header H3 { +"by value" }

        p { +"Functional Components can store state as well:" }

        PrismKotlin(
            "var x by value(100)"
        )

        p { +"Below you will find an example using this method:" }

        PrismKotlin(
            ExtractedCodeBlocks.pages_howto_kraft_code_FunctionalCounterComponent_kt_code,
        ) {
            lineNumbers()
            copyToClipboard()
        }

        p { +"And here is the FunctionalCounterComponent in action" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_howto_kraft_KraftComponentStatePage_kt_functionalByValueExample,
        ) {
            // <CodeBlock functionalByValueExample>
            ui.segment {
                FunctionalCounterComponent(100)
            }
            // </CodeBlock>
        }
    }

    private fun FlowContent.functionalBySubscribingToExample() = example {
        ui.dividing.header H3 { +"by subscribingTo" }

        p { +"Functional Components can subscribe to streams as well:" }

        PrismKotlin(
            "val x by subscribingTo(someStream)"
        )

        p { +"Below you will find an example using this method:" }

        PrismKotlin(
            ExtractedCodeBlocks.pages_howto_kraft_code_FunctionalTickerComponent_kt_code,
        ) {
            lineNumbers()
            copyToClipboard()
        }

        p { +"And here is the FunctionalTickerComponent in action" }

        HorizontalContentAndCode(
            ExtractedCodeBlocks.pages_howto_kraft_KraftComponentStatePage_kt_functionalBySubscribingToExample,
        ) {
            // <CodeBlock functionalBySubscribingToExample>
            ui.segment {
                FunctionalTickerComponent(100)
            }
            // </CodeBlock>
        }
    }
}
