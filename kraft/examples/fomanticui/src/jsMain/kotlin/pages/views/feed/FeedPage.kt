package de.peekandpoke.kraft.examples.fomanticui.pages.views.feed

import de.peekandpoke.kraft.addons.routing.PageTitle
import de.peekandpoke.kraft.components.NoProps
import de.peekandpoke.kraft.components.PureComponent
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.examples.fomanticui.helpers.readTheFomanticUiDocs
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.vdom.VDom
import kotlinx.html.Tag

@Suppress("FunctionName")
fun Tag.FeedPage() = comp {
    FeedPage(it)
}

class FeedPage(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        PageTitle("Views | Feed")

        ui.basic.segment {
            ui.dividing.header H1 { +"Feed" }

            readTheFomanticUiDocs("https://fomantic-ui.com/views/feed.html")
        }
    }
}
