package de.peekandpoke.kraft.examples.jsaddons.avatars

import de.peekandpoke.kraft.addons.routing.RouterBuilder
import de.peekandpoke.kraft.addons.routing.Static

class AvatarsRoutes {
    val index = Static("/example/minidenticons")
}

fun RouterBuilder.mount(routes: AvatarsRoutes) {
    mount(routes.index) { MinIdenticonsExample() }
}
