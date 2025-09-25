package de.peekandpoke.kraft.routing

fun router(builder: RouterBuilder.() -> Unit): Router = RouterBuilder().apply(builder).build()

typealias RouterProvider = () -> Router
