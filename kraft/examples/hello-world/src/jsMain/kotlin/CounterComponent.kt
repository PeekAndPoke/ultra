package io.peekandpoke.kraft.examples.helloworld

import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.html.onClick
import io.peekandpoke.ultra.semanticui.ui
import kotlinx.html.Tag

@Suppress("FunctionName")
fun Tag.CounterComponent(start: Int) = comp(
    CounterComponent.Props(start = start)
) {
    CounterComponent(it)
}

class CounterComponent(ctx: Ctx<Props>) : Component<CounterComponent.Props>(ctx) {

    data class Props(
        val start: Int,
    )

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    // `by value(...)` creates reactive state — changing it triggers a re-render.
    private var counter: Int by value(props.start)

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        ui.segment {
            ui.header H3 { +"Value: $counter" }

            ui.button {
                onClick { counter-- }
                +"−"
            }
            ui.blue.button {
                onClick { counter++ }
                +"+"
            }
        }
    }
}
