@file:Suppress(
    "DuplicatedCode",
    "Detekt:TooManyFunctions",
    "Detekt:LargeClass",
    "Detekt:LongMethod",
)

package io.peekandpoke.kraft.examples.fomanticui.pages.elements.container

import generated.ExtractedCodeBlocks
import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.examples.fomanticui.helpers.VerticalContentAndCode
import io.peekandpoke.kraft.examples.fomanticui.helpers.example
import io.peekandpoke.kraft.examples.fomanticui.helpers.mediaParagraphWireFrame
import io.peekandpoke.kraft.examples.fomanticui.helpers.readTheFomanticUiDocs
import io.peekandpoke.kraft.routing.PageTitle
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.html.FlowContent
import kotlinx.html.Tag

@Suppress("FunctionName")
fun Tag.ContainerPage() = comp {
    ContainerPage(it)
}

class ContainerPage(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        PageTitle("Elements | Container")

        ui.basic.segment {
            ui.dividing.header H1 { +"Header" }

            readTheFomanticUiDocs("https://fomantic-ui.com/elements/container.html")

            ui.dividing.header H2 { +"Usage" }

            renderBasicUsage()
        }
    }

    private fun FlowContent.renderBasicUsage() = example {
        ui.header H3 { +"Basic usage" }

        VerticalContentAndCode(
            ExtractedCodeBlocks.pages_elements_container_ContainerPage_kt_renderBasicUsage,
        ) {
            // <CodeBlock renderBasicUsage>
            ui.container {
                ui.segment {
                    mediaParagraphWireFrame()
                }
            }
            // </CodeBlock>
        }
    }
}
