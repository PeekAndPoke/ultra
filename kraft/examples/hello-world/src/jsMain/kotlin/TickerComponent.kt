package io.peekandpoke.kraft.examples.helloworld

import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.semanticui.ui
import io.peekandpoke.ultra.streams.ops.ticker
import kotlinx.html.Tag

@Suppress("FunctionName")
fun Tag.TickerComponent(delay: Int) = comp(
    TickerComponent.Props(delay = delay)
) {
    TickerComponent(it)
}

class TickerComponent(ctx: Ctx<Props>) : Component<TickerComponent.Props>(ctx) {

    data class Props(
        val delay: Int,
    )

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    // `ticker(delay)` creates a Stream<Int> that emits on an interval.
    private val tick = ticker(props.delay)

    // `subscribingTo(stream)` binds the property to the stream.
    // The component re-renders whenever the stream emits. Auto-unsubscribes on unmount.
    private val currentTick by subscribingTo(tick)

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        ui.segment {
            ui.header H3 { +"Tick: $currentTick" }
        }
    }
}
