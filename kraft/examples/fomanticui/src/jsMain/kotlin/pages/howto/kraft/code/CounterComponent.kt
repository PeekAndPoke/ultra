@file:Suppress("FunctionName")

package de.peekandpoke.kraft.examples.fomanticui.pages.howto.kraft.code

// <CodeBlock code>
import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.components.Ctx
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.components.onClick
import de.peekandpoke.kraft.vdom.VDom
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
