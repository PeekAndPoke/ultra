package io.peekandpoke.kraft.addons.chartjs

import io.peekandpoke.kraft.KraftDsl
import io.peekandpoke.kraft.addons.registry.Addon
import io.peekandpoke.kraft.addons.registry.AddonKey
import io.peekandpoke.kraft.addons.registry.AddonRegistry
import io.peekandpoke.kraft.addons.registry.AddonRegistryBuilder
import kotlinx.coroutines.await
import org.w3c.dom.CanvasRenderingContext2D
import kotlin.js.Promise

/**
 * Facade for the lazily-loaded Chart.js addon.
 *
 * Provides type-safe access to Chart.js chart creation and module registration.
 */
class ChartJsAddon internal constructor(
    private val chartConstructor: dynamic,
    private val registerableModules: dynamic,
) {
    private var registered = false

    /** Registers all Chart.js modules (controllers, scales, elements, plugins). Called once. */
    fun registerAll() {
        if (registered) return
        registered = true

        val ctor = chartConstructor
        val regs = registerableModules
        js("ctor.register.apply(ctor, regs)")
    }

    /** Creates a new Chart instance on the given canvas [ctx] with the given [config]. */
    fun createChart(ctx: CanvasRenderingContext2D, config: ChartConfig): Chart {
        val ctor = chartConstructor
        return js("new ctor(ctx, config)").unsafeCast<Chart>()
    }
}

/** Key for the chartjs addon. */
val chartJsAddonKey = AddonKey<ChartJsAddon>("chartjs")

/** Registers the chartjs addon for lazy loading via dynamic import. */
@KraftDsl
fun AddonRegistryBuilder.chartJs(lazy: Boolean = false): Addon<ChartJsAddon> = register(
    key = chartJsAddonKey,
    name = "chartjs",
    lazy = lazy,
) {
    @Suppress("UnsafeCastFromDynamic")
    val module: dynamic = (js("import('chart.js')") as Promise<dynamic>).await()

    ChartJsAddon(
        chartConstructor = module.Chart,
        registerableModules = module.registerables,
    )
}

/** Accessor for the chartjs addon on the registry. */
val AddonRegistry.chartJs: Addon<ChartJsAddon>
    get() = this[chartJsAddonKey]
