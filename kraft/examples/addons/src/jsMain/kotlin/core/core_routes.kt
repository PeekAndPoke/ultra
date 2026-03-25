package io.peekandpoke.kraft.examples.jsaddons.core

import io.peekandpoke.kraft.routing.RootRouterBuilder
import io.peekandpoke.kraft.routing.Static

class CoreRoutes {
    val index = Static("/core")
    val scriptLoader = Static("/core/script-loader")
}

fun RootRouterBuilder.mount(routes: CoreRoutes) {
    mount(routes.index) {}
    mount(routes.scriptLoader) { ScriptLoaderExample() }
}
