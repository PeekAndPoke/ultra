package de.peekandpoke.kraft.examples.jsaddons.core

import de.peekandpoke.kraft.addons.routing.RouterBuilder
import de.peekandpoke.kraft.addons.routing.Static

class CoreRoutes {
    val index = Static("/core")
    val scriptLoader = Static("/core/script-loader")
}

fun RouterBuilder.mount(routes: CoreRoutes) {
    mount(routes.index) {}
    mount(routes.scriptLoader) { ScriptLoaderExample() }
}
