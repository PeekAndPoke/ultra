package de.peekandpoke.kraft.routing

import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.components.Ctx
import de.peekandpoke.kraft.components.comp
import de.peekandpoke.kraft.routing.Router.Companion.router
import de.peekandpoke.kraft.vdom.VDom
import de.peekandpoke.ultra.html.key
import de.peekandpoke.ultra.streams.ops.distinct
import de.peekandpoke.ultra.streams.ops.map
import kotlinx.browser.window
import kotlinx.html.Tag
import kotlinx.html.div

/**
 * The router component displays the view that is associated with the [ActiveRoute] of the given [Router]
 */
@Suppress("FunctionName")
fun Tag.RouterComponent(
    router: Router? = null,
) = comp(RouterComponent.Props(router)) {
    RouterComponent(it)
}

class RouterComponent internal constructor(ctx: Ctx<Props>) : Component<RouterComponent.Props>(ctx) {

    data class Props(
        val router: Router?,
    )

    ////  STATE  ///////////////////////////////////////////////////////////////////////////////////////////////////////

    private val activeRouter get() = props.router ?: router

    private val current: ActiveRoute by subscribingTo(activeRouter.current)

    private val currentUri: String by subscribingTo(
        activeRouter.current
            .map { it.uri.split("?").firstOrNull() ?: "" }
            .distinct()
    ) {
        scrollUp()
    }

    ////  IMPL  ////////////////////////////////////////////////////////////////////////////////////////////////////////

    private fun scrollUp() {
        dom?.let {
            window.scrollTo(0.0, 0.0)
        }
    }

    override fun VDom.render() {
        div(classes = "router") {
            // We define a key, so that the VDomEngine does a full redraw of the content, when the matched route changes
            key = currentUri

            current.render(this)
        }
    }
}
