package de.peekandpoke.kraft.examples.jsaddons.core

import de.peekandpoke.kraft.components.NoProps
import de.peekandpoke.kraft.components.PureComponent
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.examples.jsaddons.routes
import de.peekandpoke.kraft.semanticui.noui
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.vdom.VDom
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

        ui.cards {
            noui.card A {
                href = routes.core.scriptLoader()
                +"Script Loader"
            }
        }
    }
}
