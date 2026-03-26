package io.peekandpoke.kraft.routing

import kotlinx.html.FlowContent

/** DSL marker for router builder functions. */
@DslMarker
annotation class RouterDsl

/** Creates and configures a [Router] using the DSL [builder]. */
@RouterDsl
fun router(builder: RootRouterBuilder.() -> Unit): Router = RootRouterBuilder().apply(builder).build()

/** Type alias for a layout function that wraps route content. */
typealias LayoutFn = FlowContent.(content: FlowContent.() -> Unit) -> Unit
