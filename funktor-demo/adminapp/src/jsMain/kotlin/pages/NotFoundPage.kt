package de.peekandpoke.funktor.demo.adminapp.pages

import de.peekandpoke.kraft.components.NoProps
import de.peekandpoke.kraft.components.PureComponent
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.routing.JoinedPageTitle
import de.peekandpoke.kraft.vdom.VDom
import de.peekandpoke.ultra.html.css
import de.peekandpoke.ultra.semanticui.ui
import kotlinx.css.marginTop
import kotlinx.css.vh
import kotlinx.html.Tag

@Suppress("FunctionName")
fun Tag.NotFoundPage() = comp {
    NotFoundPage(it)
}

class NotFoundPage(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        JoinedPageTitle { listOf("Not Found", "404") }

        ui.red.segment {
            css {
                marginTop = 20.vh
            }

            +"Oops, the page you were looking for was not found."
        }
    }
}
