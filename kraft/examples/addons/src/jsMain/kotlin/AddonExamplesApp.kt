package io.peekandpoke.kraft.examples.jsaddons

import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.routing.RouterComponent
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.semanticui.ui
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
