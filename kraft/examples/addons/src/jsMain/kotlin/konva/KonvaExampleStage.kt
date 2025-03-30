package de.peekandpoke.kraft.examples.jsaddons.konva

import de.peekandpoke.kraft.components.NoProps
import de.peekandpoke.kraft.components.PureComponent
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.components.key
import de.peekandpoke.kraft.semanticui.css
import de.peekandpoke.kraft.utils.jsObject
import de.peekandpoke.kraft.vdom.VDom
import konva.Circle
import konva.Layer
import konva.LayerConfig
import konva.Stage
import kotlinx.css.height
import kotlinx.css.pct
import kotlinx.css.width
import kotlinx.html.Tag
import kotlinx.html.div
import kotlin.math.max

@Suppress("FunctionName")
fun Tag.KonvaExampleStage() = comp {
    KonvaExampleStage(it)
}

class KonvaExampleStage(ctx: NoProps) : PureComponent(ctx) {

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    init {
        lifecycle {
            onMount {
                dom?.let { d ->

                    val clientWidth = d.clientWidth
                    val clientHeight = d.clientHeight

                    val stage = Stage(
                        jsObject {
                            container = d
                            width = clientWidth
                            height = clientHeight
                        }
                    )

                    var viewScale = 1.0

                    val layer = Layer(
                        jsObject<LayerConfig> {
                            listening = true
                            x = (clientWidth / 2)
                            y = (clientHeight / 2)
                        }
                    )
                    stage.add(layer)

                    layer.add(
                        Circle(
                            jsObject {
                                id = "circle"
                                x = 0
                                y = 0
                                radius = 200
                                fill = "#FFAAAA"
                                stroke = "#888888"
                                strokeWidth = 1
                            }
                        )
                    )

                    stage.on("wheel") { event ->
                        viewScale = max(0.1, viewScale + (event.evt.asDynamic().wheelDelta as Double / 2000.0))
                        layer.scaleX(viewScale)
                        layer.scaleY(viewScale)
                    }
                }
            }
        }
    }

    override fun VDom.render() {
        div {
            key = "stage"

            css {
                width = 100.pct
                height = 100.pct
            }
        }
    }
}
