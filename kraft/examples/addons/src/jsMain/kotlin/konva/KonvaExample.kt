package io.peekandpoke.kraft.examples.jsaddons.konva

import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.html.css
import io.peekandpoke.ultra.semanticui.ui
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
