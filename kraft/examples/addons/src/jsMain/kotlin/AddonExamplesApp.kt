package de.peekandpoke.kraft.examples.jsaddons

import de.peekandpoke.kraft.components.NoProps
import de.peekandpoke.kraft.components.PureComponent
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.routing.RouterComponent
import de.peekandpoke.kraft.vdom.VDom
import de.peekandpoke.ultra.semanticui.ui
import kotlinx.html.Tag

@Suppress("FunctionName")
fun Tag.AddonExamplesApp() = comp {
    AddonExamplesApp(it)
}

class AddonExamplesApp(ctx: NoProps) : PureComponent(ctx) {

    override fun VDom.render() {
        ui.container {
            ui.basic.segment {
                ui.header H1 { +"KRAFT AddOns" }
            }

            ui.basic.segment {
                RouterComponent()
            }
        }
    }
}
