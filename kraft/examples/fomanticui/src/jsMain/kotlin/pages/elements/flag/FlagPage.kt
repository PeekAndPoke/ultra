package io.peekandpoke.kraft.examples.fomanticui.pages.elements.flag

import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.examples.fomanticui.helpers.readTheFomanticUiDocs
import io.peekandpoke.kraft.routing.PageTitle
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.html.Tag

@Suppress("FunctionName")
fun Tag.FlagPage() = comp {
    FlagPage(it)
}

class FlagPage(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        PageTitle("Elements | Flag")

        ui.basic.segment {
            ui.dividing.header H1 { +"Flag" }

            readTheFomanticUiDocs("https://fomantic-ui.com/elements/flag.html")

            ui.dividing.header H2 { +"Simple Usage" }
            FlagExamples()

            ui.dividing.header H2 { +"Flag search" }
            FlagSearch()
        }
    }
}
