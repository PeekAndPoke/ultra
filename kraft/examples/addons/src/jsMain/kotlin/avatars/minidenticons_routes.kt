package de.peekandpoke.kraft.examples.jsaddons.avatars

import de.peekandpoke.kraft.routing.RootRouterBuilder
import de.peekandpoke.kraft.routing.Static

class AvatarsRoutes {
    val index = Static("/example/minidenticons")
}

fun RootRouterBuilder.mount(routes: AvatarsRoutes) {
    mount(routes.index) { MinIdenticonsExample() }
}
