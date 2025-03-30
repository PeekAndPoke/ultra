package de.peekandpoke.kraft.examples.fomanticui.pages.howto.kraft.code

// <CodeBlock code>
import de.peekandpoke.kraft.components.component
import de.peekandpoke.kraft.components.subscribingTo
import de.peekandpoke.kraft.components.value
import de.peekandpoke.kraft.streams.addons.ticker
import kotlinx.html.div

val FunctionalTickerComponent = component { delay: Int ->
    /**
     * Here we define a stream.
     * It has to be defined with 'by value' in order to remember it when the component is re-rendered.
     */
    val stream by value { ticker(delay) }

    /**
     * Here we define a property that subscribes to the stream, triggering a redraw whenever a new value
     * is published by the stream.
     */
    val counter by subscribingTo(stream)

    div {
        +"Ticker: $counter"
    }
}
// </CodeBlock>
