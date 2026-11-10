package io.peekandpoke.kraft.addons.visnetwork

/**
 * Typed views on the vis-network objects the component passes through.
 *
 * Only the common fields are typed; set any other vis-network option with `asDynamic()`. Build them with
 * `jsObject { }`. See https://visjs.github.io/vis-network/docs/network/
 */

/** The nodes and edges of a network. Plain arrays; vis-network wraps them in its own data sets. */
external interface VisNetworkData {
    var nodes: Array<VisNode>
    var edges: Array<VisEdge>
}

/** One node. Its [label] is drawn on the canvas, never as markup. */
external interface VisNode {
    var id: Any
    var label: String?

    /** Size, scaled by the library between `nodes.scaling.min` and `max`. */
    var value: Number?
    var color: String?
    var shape: String?

    /**
     * A tooltip. **A string is shown as text**; vis-network renders HTML only for an element passed here, so
     * never build one from untrusted input.
     */
    var title: Any?
}

/** One edge, from node id [from] to node id [to]. An edge to an id with no node is not drawn. */
external interface VisEdge {
    var id: Any?
    var from: Any
    var to: Any
    var label: String?

    /** For example `"to"`, `"from"` or `"to, from"`. */
    var arrows: String?
    var dashes: Boolean?

    /** Width, scaled by the library between `edges.scaling.min` and `max`. */
    var value: Number?
    var title: Any?
}

/** The network options: `layout`, `physics`, `nodes`, `edges`, `interaction`, ... */
external interface VisNetworkOptions {
    var layout: dynamic
    var physics: dynamic
    var nodes: dynamic
    var edges: dynamic
    var interaction: dynamic
}

/** A drawn network. */
external interface VisNetwork {
    /** Replaces all nodes and edges. */
    fun setData(data: VisNetworkData)

    /** Merges [options] into the current ones. */
    fun setOptions(options: VisNetworkOptions)

    /** Zooms so that every node is visible. */
    fun fit()

    /** The positions of all nodes, by node id. */
    fun getPositions(): dynamic

    /** Listens to a network event such as `"click"` or `"stabilized"`. */
    fun on(event: String, callback: (params: dynamic) -> Unit)

    /** Removes the network from the page and frees its resources. */
    fun destroy()
}
