@file:Suppress("FunctionName")

package io.peekandpoke.kraft.examples.fomanticui.pages.howto.kraft.code

// <CodeBlock code>
import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.html.onClick
import kotlinx.html.Tag
import kotlinx.html.button
import kotlinx.html.div

fun Tag.CounterComponent(start: Int) = comp(
    CounterComponent.Props(start = start)
) {
    CounterComponent(it)
}

class CounterComponent(ctx: Ctx<Props>) : Component<CounterComponent.Props>(ctx) {

    data class Props(
        val start: Int,
    )

    /** Here we define a property that will trigger a redraw when it's value is changed */
    private var counter: Int by value(props.start)

    override fun VDom.render() {
        div {
            div { +"Value: $counter" }
            div {
                /** Below we change our property, triggering a redraw */
                button { onClick { counter-- }; +" - " }
                button { onClick { counter++ }; +" + " }
            }
        }
    }
}
// </CodeBlock>
