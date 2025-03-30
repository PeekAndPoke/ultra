@file:Suppress(
    "DuplicatedCode",
    "Detekt:TooManyFunctions",
    "Detekt:LargeClass",
    "Detekt:LongMethod",
)

package de.peekandpoke.kraft.examples.fomanticui.pages.howto.kraft

import de.peekandpoke.kraft.addons.prismjs.PrismHtml
import de.peekandpoke.kraft.addons.prismjs.PrismKotlin
import de.peekandpoke.kraft.addons.prismjs.PrismPlugin.CopyToClipboard.Companion.copyToClipboard
import de.peekandpoke.kraft.addons.prismjs.PrismPlugin.LineNumbers.Companion.lineNumbers
import de.peekandpoke.kraft.addons.routing.PageTitle
import de.peekandpoke.kraft.components.NoProps
import de.peekandpoke.kraft.components.PureComponent
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.examples.fomanticui.helpers.example
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.vdom.VDom
import kotlinx.html.FlowContent
import kotlinx.html.Tag
import kotlinx.html.a
import kotlinx.html.p

@Suppress("FunctionName")
fun Tag.KraftGettingStartedPage() = comp {
    KraftGettingStartedPage(it)
}

class KraftGettingStartedPage(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        PageTitle("Kraft | Getting Started")

        ui.basic.padded.segment {
            ui.dividing.header H1 { +"KRAFT - getting started" }

            ui.info.message {
                p {
                    +"A good starting point is to look at the hello-world example"
                }
                p {
                    +"You can find the code here "
                    a(href = "https://github.com/PeekAndPoke/kraft/tree/master/examples/hello-world") { +"here" }
                }
            }

            renderExample()
        }
    }

    private fun FlowContent.renderExample() = example {
        ui.header H2 { +"Foundation" }

        ui.top.info.message {
            +"Generally speaking you first need an HTML page which might look like this:"
        }

        PrismHtml(
            """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>My App</title>
    <link rel="stylesheet"
          href="https://cdnjs.cloudflare.com/ajax/libs/fomantic-ui/2.9.0/semantic.min.css"
          integrity="sha512-PwhgdrueUt7iVICnZMjYcbiLalCztrVfzUIYXekIK8hZu4DQP141GrKh6fUHmNERWi4bGdBXIZqtBZnsSzHEMg=="
          crossorigin="anonymous"
          referrerpolicy="no-referrer"/>
</head>
<div id="spa"></div>

<script src="my-app.js"></script>

</body>
</html>                
            """.trimIndent()
        ) {
            lineNumbers()
            copyToClipboard()
        }

        ui.warning.message {
            +"Make sure that the include javascript package (in this case 'my-app.js') matches your projects output."
        }

        ui.divider()

        ui.top.info.message {
            +"Then you will need to run the SPA from kotlin. The basic code for this could look like this:"
        }

        PrismKotlin(
            """
import de.peekandpoke.kraft.Kraft
import de.peekandpoke.kraft.vdom.preact.PreactVDomEngine
import kotlinx.browser.document
import kotlinx.html.h1
import org.w3c.dom.HTMLElement

/** Initializes KRAFT */
val kraft = Kraft.initialize()

fun main() {
    // Get the DOM-Element into which the app should be mounted
    val mountPoint = document.getElementById("spa") as HTMLElement

    // Initialize the VDOM-engine (in this case Preact) and render some content 
    PreactVDomEngine(mountPoint) {
        h1 { +"Hello World"}
    }
}
            """.trimIndent()
        ) {
            lineNumbers()
            copyToClipboard()
        }
    }

}
