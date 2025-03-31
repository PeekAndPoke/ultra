package de.peekandpoke.kraft.examples.fomanticui.pages.elements.flag

import de.peekandpoke.kraft.addons.routing.PageTitle
import de.peekandpoke.kraft.components.NoProps
import de.peekandpoke.kraft.components.PureComponent
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.examples.fomanticui.helpers.readTheFomanticUiDocs
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.vdom.VDom
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
