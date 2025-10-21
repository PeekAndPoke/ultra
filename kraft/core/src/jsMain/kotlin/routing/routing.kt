package de.peekandpoke.kraft.routing

import de.peekandpoke.kraft.KraftApp
import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.components.getAttributeRecursive
import kotlinx.html.FlowContent

@DslMarker
annotation class RouterDsl

@RouterDsl
fun router(builder: RootRouterBuilder.() -> Unit): Router = RootRouterBuilder().apply(builder).build()

@RouterDsl
val KraftApp.router: Router get() = appAttributes[Router.key]!!

@RouterDsl
val Component<*>.router get() = getAttributeRecursive(Router.key)

typealias LayoutFn = FlowContent.(content: FlowContent.() -> Unit) -> Unit
