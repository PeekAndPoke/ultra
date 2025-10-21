package de.peekandpoke.kraft.examples.jsaddons.core

import de.peekandpoke.kraft.routing.RootRouterBuilder
import de.peekandpoke.kraft.routing.Static

class CoreRoutes {
    val index = Static("/core")
    val scriptLoader = Static("/core/script-loader")
}

fun RootRouterBuilder.mount(routes: CoreRoutes) {
    mount(routes.index) {}
    mount(routes.scriptLoader) { ScriptLoaderExample() }
}
