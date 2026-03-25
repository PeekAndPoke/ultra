package io.peekandpoke.kraft.examples.fomanticui.pages.views.advertisement

import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.examples.fomanticui.helpers.readTheFomanticUiDocs
import io.peekandpoke.kraft.routing.PageTitle
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.semanticui.ui
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
