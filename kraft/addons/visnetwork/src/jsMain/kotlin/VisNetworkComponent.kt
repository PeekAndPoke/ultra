package io.peekandpoke.kraft.addons.visnetwork

import io.peekandpoke.kraft.addons.registry.AddonRegistry
import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.components.getAttributeRecursiveOrNull
import io.peekandpoke.kraft.utils.launch
import io.peekandpoke.kraft.vdom.VDom
import kotlinx.dom.clear
import kotlinx.html.Tag
import kotlinx.html.div
import kotlinx.html.style

/**
 * Draws a vis-network graph of [data] with [options], [height] high and the full width.
 *
 * Uses the registered `visNetwork()` addon, or loads the library itself when the app registered none. Renders an
 * empty box until the library is there. A new [data] or [options] (by identity) redraws the live network, so build
 * them once rather than in `render()`; after a failed load or draw, it is the next try.
 * [onReady] receives the network once it is drawn, for example to listen to its events.
 *
 * Mount it only while visible: a network created in a hidden container cannot lay itself out.
 */
@Suppress("FunctionName")
fun Tag.VisNetwork(
    data: VisNetworkData,
    options: VisNetworkOptions,
    height: String = "600px",
    onReady: ((VisNetwork) -> Unit)? = null,
) = comp(
    VisNetworkComponent.Props(data = data, options = options, height = height, onReady = onReady)
) {
    VisNetworkComponent(it)
}

/** Kraft component that owns one vis-network instance. */
class VisNetworkComponent(ctx: Ctx<Props>) : Component<VisNetworkComponent.Props>(ctx) {

    ////  PROPS  //////////////////////////////////////////////////////////////////////////////////////////////////

    data class Props(
        val data: VisNetworkData,
        val options: VisNetworkOptions,
        val height: String,
        val onReady: ((VisNetwork) -> Unit)?,
    )

    ////  STATE  //////////////////////////////////////////////////////////////////////////////////////////////////

    /** The app's registered addon; null when it registered none and the component loads the library itself. */
    private val registered = getAttributeRecursiveOrNull(AddonRegistry.key)?.getOrNull(visNetworkAddonKey)

    private val addon: VisNetworkAddon? by subscribingTo(registered ?: VisNetworkLoader.stream)

    private var problem: String? by value(null)

    private var network: VisNetwork? = null

    ////  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    init {
        loadItself()

        lifecycle {
            onMount { tryCreate() }

            onUpdate { tryCreate() }

            onNextProps { new, old ->
                val live = network

                if (live == null) {
                    // A failed load or draw is tried again with the new graph
                    if (problem != null && (new.data !== old.data || new.options !== old.options)) {
                        problem = null
                        loadItself()
                    }
                    return@onNextProps
                }

                try {
                    if (new.options !== old.options) live.setOptions(new.options)
                    if (new.data !== old.data) live.setData(new.data)
                } catch (e: Throwable) {
                    console.error("Updating the network failed", e)
                }
            }

            onUnmount { destroy() }
        }
    }

    override fun VDom.render() {
        val reason = problem

        if (reason != null) {
            div(classes = "vis-network-problem") { +"The graph could not be drawn: $reason" }
        } else {
            // vis-network fills this box with its own elements; it has no children of ours
            div(classes = "vis-network-box") {
                style = "width: 100%; height: ${props.height};"
            }
        }
    }

    /** Without a registered addon, a failed load is reported here; the stream alone would stay empty. */
    private fun loadItself() {
        if (registered != null || addon != null) return

        launch {
            try {
                VisNetworkLoader.load()
            } catch (e: Throwable) {
                problem = "vis-network could not be loaded: ${e.message ?: e}"
            }
        }
    }

    private fun tryCreate() {
        val lib = addon ?: return
        if (network != null || problem != null) return

        val box = dom ?: return

        try {
            network = lib.createNetwork(box, props.data, props.options).also { props.onReady?.invoke(it) }
        } catch (e: Throwable) {
            console.error("Creating the network failed", e)
            // vis-network may have built part of its elements before throwing; preact would keep them
            box.clear()
            problem = e.message ?: e.toString()
        }
    }

    private fun destroy() {
        network?.destroy()
        network = null
    }
}
