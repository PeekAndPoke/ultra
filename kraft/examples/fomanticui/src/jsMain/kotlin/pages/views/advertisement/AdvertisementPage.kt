package de.peekandpoke.kraft.examples.fomanticui.pages.views.advertisement

import de.peekandpoke.kraft.addons.routing.PageTitle
import de.peekandpoke.kraft.components.NoProps
import de.peekandpoke.kraft.components.PureComponent
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.examples.fomanticui.helpers.readTheFomanticUiDocs
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.vdom.VDom
import kotlinx.html.Tag

@Suppress("FunctionName")
fun Tag.AdvertisementPage() = comp {
    AdvertisementPage(it)
}

class AdvertisementPage(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        PageTitle("Views | Advertisement")

        ui.basic.segment {
            ui.dividing.header H1 { +"Advertisement" }

            readTheFomanticUiDocs("https://fomantic-ui.com/views/advertisement.html")
        }
    }
}
