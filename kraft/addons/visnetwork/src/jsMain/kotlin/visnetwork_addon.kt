package io.peekandpoke.kraft.addons.visnetwork

import io.peekandpoke.kraft.addons.registry.Addon
import io.peekandpoke.kraft.addons.registry.AddonKey
import io.peekandpoke.kraft.addons.registry.AddonRegistry
import io.peekandpoke.kraft.addons.registry.AddonRegistryBuilder
import io.peekandpoke.kraft.utils.launch
import io.peekandpoke.ultra.streams.Stream
import io.peekandpoke.ultra.streams.StreamSource
import kotlinx.coroutines.await
import org.w3c.dom.HTMLElement
import kotlin.js.Promise

/**
 * Facade for the lazily-loaded vis-network library.
 *
 * Loads `vis-network/standalone`, which bundles its own `vis-data` and injects a `<style>` for its tooltip and
 * navigation widgets.
 */
class VisNetworkAddon internal constructor(
    private val networkConstructor: dynamic,
) {
    /** Draws a new network into [container]. Call [VisNetwork.destroy] when done with it. */
    fun createNetwork(container: HTMLElement, data: VisNetworkData, options: VisNetworkOptions): VisNetwork {
        @Suppress("UnusedVariable", "unused")
        val ctor = networkConstructor

        return js("new ctor(container, data, options)").unsafeCast<VisNetwork>()
    }
}

/** Key for the vis-network addon. */
val visNetworkAddonKey = AddonKey<VisNetworkAddon>("visNetwork")

/** Registers the vis-network addon for loading via dynamic import. */
fun AddonRegistryBuilder.visNetwork(lazy: Boolean = false): Addon<VisNetworkAddon> = register(
    key = visNetworkAddonKey,
    name = "visNetwork",
    lazy = lazy,
) {
    VisNetworkLoader.load()
}

/** Accessor for the vis-network addon on the registry. */
val AddonRegistry.visNetwork: Addon<VisNetworkAddon>
    get() = this[visNetworkAddonKey]

/**
 * Loads the library once per page, for the registered addon and for a [VisNetworkComponent] in an app that
 * registered none (a library page, like the funktor ops UI, cannot require its host app to register addons).
 */
internal object VisNetworkLoader {

    private var importPromise: Promise<dynamic>? = null

    private val source = StreamSource<VisNetworkAddon?>(null)

    private var started = false

    /** Emits null until the library is loaded; subscribing starts the load. */
    val stream: Stream<VisNetworkAddon?> = object : Stream<VisNetworkAddon?> {
        override fun invoke(): VisNetworkAddon? = source()

        override fun subscribeToStream(sub: (VisNetworkAddon?) -> Unit): () -> Unit {
            start()
            return source.subscribeToStream(sub)
        }
    }

    @Suppress("UnsafeCastFromDynamic")
    suspend fun load(): VisNetworkAddon {
        source()?.let { return it }

        val promise = importPromise ?: (js("import('vis-network/standalone')") as Promise<dynamic>).also {
            importPromise = it
        }

        val module: dynamic = try {
            promise.await()
        } catch (e: Throwable) {
            // A failed download is asked again next time, not remembered
            importPromise = null
            throw e
        }

        return source() ?: VisNetworkAddon(networkConstructor = module.Network).also { source(it) }
    }

    private fun start() {
        if (started) return
        started = true

        launch {
            try {
                load()
            } catch (e: Throwable) {
                started = false
                console.error("Failed to load vis-network: ${e.message}")
            }
        }
    }
}
