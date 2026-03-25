package io.peekandpoke.kraft.examples.fomanticui.pages.howto.blocks.utils

import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.ext.ResizeObserverEntry
import io.peekandpoke.kraft.routing.PageTitle
import io.peekandpoke.kraft.utils.onResize
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.html.Tag
import kotlinx.html.pre

@Suppress("FunctionName")
fun Tag.KraftUtilsPage() = comp {
    KraftUtilsPage(it)
}

class KraftUtilsPage(ctx: NoProps) : PureComponent(ctx) {

    private var lastResizeEvents: Array<ResizeObserverEntry>? by value(null)

    init {
        lifecycle {
            onResize {
                lastResizeEvents = it
            }
        }
    }

    override fun VDom.render() {
        PageTitle("How To | Kraft Utils")

        ui.basic.padded.segment {
            ui.header H2 { +"Resize Controller" }

            ui.message {
                +"Resize the window to see the resize events. Check console for details."
            }

            lastResizeEvents?.let { entries ->
                pre {
                    entries.forEach { evt ->
                        console.log(evt)

                        val lines = listOf(
                            "EVT: $evt",
                            "target: ${evt.target}",
                            "contentRect: ${evt.contentRect}",
                            "contentBoxSize: ${evt.contentBoxSize}",
                            "borderBoxSize: ${evt.borderBoxSize}",
                            "devicePixelContentBoxSize: ${evt.devicePixelContentBoxSize}",
                        )
                        +lines.joinToString("\n")
                        +"\n\n"
                    }
                }
            }
        }
    }
}
