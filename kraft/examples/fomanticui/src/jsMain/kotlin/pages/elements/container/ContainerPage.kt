@file:Suppress(
    "DuplicatedCode",
    "Detekt:TooManyFunctions",
    "Detekt:LargeClass",
    "Detekt:LongMethod",
)

package de.peekandpoke.kraft.examples.fomanticui.pages.elements.container

import de.peekandpoke.kraft.addons.routing.PageTitle
import de.peekandpoke.kraft.components.NoProps
import de.peekandpoke.kraft.components.PureComponent
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.examples.fomanticui.helpers.VerticalContentAndCode
import de.peekandpoke.kraft.examples.fomanticui.helpers.example
import de.peekandpoke.kraft.examples.fomanticui.helpers.mediaParagraphWireFrame
import de.peekandpoke.kraft.examples.fomanticui.helpers.readTheFomanticUiDocs
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.vdom.VDom
import generated.ExtractedCodeBlocks
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
