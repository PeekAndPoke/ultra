package io.peekandpoke.kraft.examples.helloworld

import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.streams.ops.ticker
import kotlinx.html.Tag
import kotlinx.html.div

@Suppress("FunctionName")
fun Tag.TickerComponent(
    delay: Int,
) = comp(
    TickerComponent.Props(delay = delay)
) {
    TickerComponent(it)
}

class TickerComponent(ctx: Ctx<Props>) : Component<TickerComponent.Props>(ctx) {

    //  PROPS  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class Props(
        val delay: Int,
    )

    //  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    private val tick = ticker(props.delay)

    private val currentTick by subscribingTo(tick)

    //  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    override fun VDom.render() {
        div {
            +"Ticker: $currentTick"
        }
    }
}
