package de.peekandpoke.kraft.routing

import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.components.getAttributeRecursive

fun router(builder: RouterBuilder.() -> Unit): Router = RouterBuilder().apply(builder).build()

typealias RouterProvider = () -> Router

val Component<*>.router get() = getAttributeRecursive(Router.key)
