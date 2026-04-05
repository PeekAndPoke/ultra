package io.peekandpoke.kraft.examples.helloworld

import io.peekandpoke.kraft.components.NoProps
import io.peekandpoke.kraft.components.PureComponent
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.html.Tag
import kotlinx.html.p

@Suppress("FunctionName")
fun Tag.MainPage() = comp {
    MainPage(it)
}

class MainPage(ctx: NoProps) : PureComponent(ctx) {

    override fun VDom.render() {
        ui.container {
            ui.basic.segment {
                ui.header H1 { +"Hello, Kraft!" }

                p {
                    +"A minimal showcase of Kraft's core concepts: components, state, props, "
                    +"and stream subscriptions."
                }

                ui.divider {}

                ui.header H2 { +"Counter — state and events" }

                p { +"Two counters share the same component, with different initial props." }

                CounterComponent(start = 0)
                CounterComponent(start = 10)

                ui.divider {}

                ui.header H2 { +"Ticker — stream subscription" }

                p {
                    +"A component that subscribes to a stream. "
                    +"When the stream emits, the component re-renders automatically."
                }

                TickerComponent(delay = 1000)
            }
        }
    }
}
