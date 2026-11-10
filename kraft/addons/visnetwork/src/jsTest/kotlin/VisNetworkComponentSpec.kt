package io.peekandpoke.kraft.addons.visnetwork

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.kraft.addons.registry.addons
import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.Ctx
import io.peekandpoke.kraft.components.comp
import io.peekandpoke.kraft.testing.TestBed
import io.peekandpoke.kraft.testing.eventually
import io.peekandpoke.kraft.testing.selectCss
import io.peekandpoke.kraft.utils.jsObject
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.ultra.streams.StreamSource
import js.objects.Object
import kotlinx.html.Tag
import kotlinx.html.div
import kotlinx.html.style

private class Shown(val data: VisNetworkData?)

// Host that renders VisNetwork with whatever the stream holds, or nothing
@Suppress("TestFunctionName")
private fun Tag.NetworkHost(source: StreamSource<Shown>, ready: (VisNetwork) -> Unit) = comp(
    NetworkHost.Props(source = source, ready = ready)
) {
    NetworkHost(it)
}

private class NetworkHost(ctx: Ctx<Props>) : Component<NetworkHost.Props>(ctx) {

    data class Props(
        val source: StreamSource<Shown>,
        val ready: (VisNetwork) -> Unit,
    )

    private val shown by subscribingTo(props.source)

    private val options = jsObject<VisNetworkOptions> {
        layout = jsObject<dynamic> { improvedLayout = false }
        physics = false
    }

    override fun VDom.render() {
        div {
            style = "width: 400px; height: 300px;"
            shown.data?.let { VisNetwork(data = it, options = options, height = "300px", onReady = props.ready) }
        }
    }
}

private fun network(nodeCount: Int) = jsObject<VisNetworkData> {
    nodes = Array(nodeCount) { i ->
        jsObject {
            id = i + 1
            label = "<img src=x onerror=alert(1)> node ${i + 1}"
        }
    }
    edges = Array(nodeCount - 1) { i ->
        jsObject {
            from = i + 1
            to = i + 2
            arrows = "to"
        }
    }
}

private fun VisNetwork.nodeCount(): Int = Object.keys(getPositions().unsafeCast<Any>()).size

class VisNetworkComponentSpec : StringSpec() {

    private suspend fun runWith(registered: Boolean) {
        val source = StreamSource(Shown(network(3)))
        var drawn: VisNetwork? = null

        TestBed.preact(
            appSetup = { if (registered) addons { visNetwork() } },
            view = { NetworkHost(source) { drawn = it } },
        ) { root ->
            eventually { drawn?.nodeCount() shouldBe 3 }

            // vis-network draws on a canvas: the label is no markup on the page
            root.selectCss("canvas").count() shouldBe 1
            root.selectCss("img").count() shouldBe 0
            root.selectCss(".vis-network-problem").count() shouldBe 0

            // New data updates the live network, without a second one
            val first = drawn
            source(Shown(network(5)))
            eventually { first?.nodeCount() shouldBe 5 }
            (drawn === first) shouldBe true
            root.selectCss("canvas").count() shouldBe 1

            // Unmounting destroys it. Spied: preact removing the box would hide a network left running.
            var destroyed = 0
            val live = first.asDynamic()
            val original = live.destroy
            live.destroy = { destroyed++; original.call(live) }

            source(Shown(null))
            eventually { destroyed shouldBe 1 }
            root.selectCss(".vis-network").count() shouldBe 0
        }
    }

    init {
        // First: the loader is page-wide, so after the registered case this one would find the library loaded
        "loads the library itself when the app registered no addon" {
            runWith(registered = false)
        }

        "draws, updates and destroys a network with the addon registered" {
            runWith(registered = true)
        }
    }
}
