package de.peekandpoke.kraft.examples.fomanticui.pages

import de.peekandpoke.kraft.addons.routing.PageTitle
import de.peekandpoke.kraft.components.NoProps
import de.peekandpoke.kraft.components.PureComponent
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.vdom.VDom
import kotlinx.html.Tag

@Suppress("FunctionName")
fun Tag.NotFoundPage() = comp {
    NotFoundPage(it)
}

class NotFoundPage(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        PageTitle("Oops ... Not Found")

        ui.basic.segment {
            ui.header H1 {
                +"Oops ... this page does not exist."
            }
        }
    }
}
