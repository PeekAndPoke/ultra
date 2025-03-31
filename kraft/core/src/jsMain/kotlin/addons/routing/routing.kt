package de.peekandpoke.kraft.addons.routing

fun router(builder: RouterBuilder.() -> Unit): Router = RouterBuilder().apply(builder).build()

typealias RouterProvider = () -> Router
