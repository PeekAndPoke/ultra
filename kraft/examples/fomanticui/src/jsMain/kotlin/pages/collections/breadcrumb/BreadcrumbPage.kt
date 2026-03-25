package io.peekandpoke.kraft.examples.fomanticui.pages.collections.breadcrumb

import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.examples.fomanticui.helpers.readTheFomanticUiDocs
import io.peekandpoke.kraft.routing.PageTitle
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.semanticui.ui
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
