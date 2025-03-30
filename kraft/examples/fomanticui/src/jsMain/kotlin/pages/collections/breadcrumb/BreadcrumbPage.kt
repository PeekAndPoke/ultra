package de.peekandpoke.kraft.examples.fomanticui.pages.collections.breadcrumb

import de.peekandpoke.kraft.addons.routing.PageTitle
import de.peekandpoke.kraft.components.NoProps
import de.peekandpoke.kraft.components.PureComponent
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.examples.fomanticui.helpers.readTheFomanticUiDocs
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.vdom.VDom
import kotlinx.html.Tag

@Suppress("FunctionName")
fun Tag.BreadcrumbPage() = comp {
    BreadcrumbPage(it)
}

class BreadcrumbPage(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        PageTitle("Collections | Breadcrumb")

        ui.basic.segment {
            ui.dividing.header H1 { +"Breadcrumb" }

            readTheFomanticUiDocs("https://fomantic-ui.com/collections/breadcrumb.html")
        }
    }
}
