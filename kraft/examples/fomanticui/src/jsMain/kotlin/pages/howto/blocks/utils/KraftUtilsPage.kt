package de.peekandpoke.kraft.examples.fomanticui.pages.howto.blocks.utils

import de.peekandpoke.kraft.components.NoProps
import de.peekandpoke.kraft.components.PureComponent
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.ext.ResizeObserverEntry
import de.peekandpoke.kraft.routing.PageTitle
import de.peekandpoke.kraft.utils.onResize
import de.peekandpoke.kraft.vdom.VDom
import de.peekandpoke.ultra.semanticui.ui
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
