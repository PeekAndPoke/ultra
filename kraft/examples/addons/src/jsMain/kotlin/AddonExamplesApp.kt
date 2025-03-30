package de.peekandpoke.kraft.examples.jsaddons

import de.peekandpoke.kraft.addons.routing.RouterComponent
import de.peekandpoke.kraft.components.NoProps
import de.peekandpoke.kraft.components.PureComponent
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.vdom.VDom
import kotlinx.html.Tag

@Suppress("FunctionName")
fun Tag.AddonExamplesApp() = comp {
    AddonExamplesApp(it)
}

class AddonExamplesApp(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    @Suppress("unused")
    private val activeRoute by subscribingTo(router.current)

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {

        ui.container {

            ui.basic.segment {
                ui.header H1 { +"KRAFT Usage Examples" }
            }

            ui.basic.segment {
                RouterComponent(router = router)
            }
        }
    }
}
