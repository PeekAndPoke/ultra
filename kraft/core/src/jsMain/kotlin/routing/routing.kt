package de.peekandpoke.kraft.routing

import kotlinx.html.FlowContent

@DslMarker
annotation class RouterDsl

@RouterDsl
fun router(builder: RootRouterBuilder.() -> Unit): Router = RootRouterBuilder().apply(builder).build()

typealias LayoutFn = FlowContent.(content: FlowContent.() -> Unit) -> Unit
