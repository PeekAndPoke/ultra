package io.peekandpoke.kraft.examples.jsaddons.konva

import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.utils.jsObject
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.html.css
import io.peekandpoke.ultra.html.key
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
