package io.peekandpoke.kraft.examples.jsaddons.core

import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.examples.jsaddons.routes
import io.peekandpoke.kraft.routing.href
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.semanticui.noui
import io.peekandpoke.ultra.semanticui.ui
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
