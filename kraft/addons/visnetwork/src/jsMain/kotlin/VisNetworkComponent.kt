package io.peekandpoke.kraft.addons.visnetwork

import io.peekandpoke.kraft.addons.registry.AddonRegistry
import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.components.getAttributeRecursiveOrNull
import io.peekandpoke.kraft.vdom.VDom
import kotlinx.html.Tag
import kotlinx.html.div
import kotlinx.html.style

/**
 * Draws a vis-network graph of [data] with [options], [height] high and the full width.
 *
 * Uses the registered `visNetwork()` addon, or loads the library itself when the app registered none. Renders an
 * empty box until the library is there. A new [data] or [options] (by identity) updates the live network.
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

    private val addon: VisNetworkAddon? by subscribingTo(
        getAttributeRecursiveOrNull(AddonRegistry.key)?.getOrNull(visNetworkAddonKey) ?: VisNetworkLoader.stream
    )

    private var problem: String? by value(null)

    private var network: VisNetwork? = null

    ////  IMPL  ///////////////////////////////////////////////////////////////////////////////////////////////////

    init {
        lifecycle {
            onMount { tryCreate() }

            onUpdate { tryCreate() }

            onNextProps { new, old ->
                val live = network ?: return@onNextProps

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

    private fun tryCreate() {
        val lib = addon ?: return
        if (network != null || problem != null) return

        val box = dom ?: return

        try {
            network = lib.createNetwork(box, props.data, props.options).also { props.onReady?.invoke(it) }
        } catch (e: Throwable) {
            console.error("Creating the network failed", e)
            problem = e.message ?: e.toString()
        }
    }

    private fun destroy() {
        network?.destroy()
        network = null
    }
}
