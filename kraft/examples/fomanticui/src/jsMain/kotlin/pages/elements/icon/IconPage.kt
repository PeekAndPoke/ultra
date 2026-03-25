package io.peekandpoke.kraft.examples.fomanticui.pages.elements.icon

import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.examples.fomanticui.helpers.readTheFomanticUiDocs
import io.peekandpoke.kraft.routing.PageTitle
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.html.Tag

@Suppress("FunctionName")
fun Tag.IconPage() = comp {
    IconPage(it)
}

class IconPage(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        PageTitle("Elements | Icon")

        ui.basic.segment {
            ui.dividing.header H1 { +"Icon" }

            readTheFomanticUiDocs("https://fomantic-ui.com/elements/icon.html#/definition")

            IconExamples()

            ui.dividing.header H2 { +"Icon search" }
            IconSearch()
        }
    }
}
