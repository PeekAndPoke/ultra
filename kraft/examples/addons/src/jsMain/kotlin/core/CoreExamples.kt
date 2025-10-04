package de.peekandpoke.kraft.examples.jsaddons.core

import de.peekandpoke.kraft.components.NoProps
import de.peekandpoke.kraft.components.PureComponent
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.examples.jsaddons.routes
import de.peekandpoke.kraft.routing.href
import de.peekandpoke.kraft.vdom.VDom
import de.peekandpoke.ultra.semanticui.noui
import de.peekandpoke.ultra.semanticui.ui
import kotlinx.html.Tag

@Suppress("FunctionName")
fun Tag.CoreExamples() = comp {
    CoreExamples(it)
}

class CoreExamples(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        ui.header H2 { +"KRAFT Core examples" }

        ui.four.doubling.cards {
            noui.card A {
                href(routes.core.scriptLoader())
                noui.content {
                    +"Script Loader"
                }
            }
        }
    }
}
