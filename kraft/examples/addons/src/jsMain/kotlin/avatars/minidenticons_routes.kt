package io.peekandpoke.kraft.examples.jsaddons.avatars

import io.peekandpoke.kraft.routing.RootRouterBuilder
import io.peekandpoke.kraft.routing.Static

class AvatarsRoutes {
    val index = Static("/example/minidenticons")
}

fun RootRouterBuilder.mount(routes: AvatarsRoutes) {
    mount(routes.index) { MinIdenticonsExample() }
}
