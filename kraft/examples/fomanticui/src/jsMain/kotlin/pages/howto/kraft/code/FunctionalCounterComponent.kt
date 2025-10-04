package de.peekandpoke.kraft.examples.fomanticui.pages.howto.kraft.code

// <CodeBlock code>
import de.peekandpoke.kraft.components.component
import de.peekandpoke.kraft.components.value
import de.peekandpoke.ultra.html.onClick
import kotlinx.html.button
import kotlinx.html.div

val FunctionalCounterComponent = component { start: Int ->

    /** Here we define a property that will trigger a redraw when it's value is changed */
    var counter: Int by value { start }

    div {
        div { +"Value: $counter" }
        div {
            /** Below we change our property, triggering a redraw */
            button { onClick { counter-- }; +" - " }
            button { onClick { counter++ }; +" + " }
        }
    }
}
// </CodeBlock>
