package de.peekandpoke.kraft.examples.jsaddons.konva

import de.peekandpoke.kraft.components.NoProps
import de.peekandpoke.kraft.components.PureComponent
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.semanticui.css
import de.peekandpoke.kraft.semanticui.ui
import de.peekandpoke.kraft.vdom.VDom
import kotlinx.css.height
import kotlinx.css.pct
import kotlinx.css.vh
import kotlinx.css.width
import kotlinx.html.Tag
import kotlinx.html.a
import kotlinx.html.div

@Suppress("FunctionName")
fun Tag.KonvaExample() = comp {
    KonvaExample(it)
}

class KonvaExample(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {

        ui.segment {
            ui.header H2 { +"Konva" }

            div {
                a(href = "https://konvajs.org/") {
                    +"Docs"
                }
            }

            div {
                css {
                    width = 100.pct
                    height = 50.vh
                }
                KonvaExampleStage()
            }
        }
    }
}
