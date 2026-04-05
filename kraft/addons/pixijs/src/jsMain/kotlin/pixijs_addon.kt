package io.peekandpoke.kraft.addons.pixijs

import io.peekandpoke.kraft.KraftDsl
import io.peekandpoke.kraft.addons.pixijs.js.Application
import io.peekandpoke.kraft.addons.pixijs.js.Graphics
import io.peekandpoke.kraft.addons.pixijs.js.Text
import io.peekandpoke.kraft.addons.pixijs.js.TextOptions
import io.peekandpoke.kraft.addons.registry.Addon
import io.peekandpoke.kraft.addons.registry.AddonKey
import io.peekandpoke.kraft.addons.registry.AddonRegistry
import io.peekandpoke.kraft.addons.registry.AddonRegistryBuilder
import kotlinx.coroutines.await
import kotlin.js.Promise

/**
 * Facade for the lazily-loaded pixi.js addon.
 *
 * Provides type-safe access to PixiJS v8 for 2D WebGL/WebGPU rendering.
 */
class PixiJsAddon internal constructor(
    private val pixiModule: dynamic,
) {
    /** Creates a new, uninitialized [Application]. Call `.init(options).await()` before use. */
    fun createApplication(): Application {
        @Suppress("UnusedVariable", "unused")
        val ctor = pixiModule.Application
        return js("new ctor()").unsafeCast<Application>()
    }

    /** Creates a new [Graphics] instance. */
    fun createGraphics(): Graphics {
        @Suppress("UnusedVariable", "unused")
        val ctor = pixiModule.Graphics
        return js("new ctor()").unsafeCast<Graphics>()
    }

    /** Creates a new [Text] instance. */
    fun createText(options: TextOptions): Text {
        @Suppress("UnusedVariable", "unused")
        val ctor = pixiModule.Text
        return js("new ctor(options)").unsafeCast<Text>()
    }
}

/** Key for the pixi.js addon. */
val pixiJsAddonKey = AddonKey<PixiJsAddon>("pixijs")

/** Registers the pixi.js addon for lazy loading via dynamic import. */
@KraftDsl
fun AddonRegistryBuilder.pixiJs(lazy: Boolean = false): Addon<PixiJsAddon> = register(
    key = pixiJsAddonKey,
    name = "pixijs",
    lazy = lazy,
) {
    @Suppress("UnsafeCastFromDynamic")
    val module: dynamic = (js("import('pixi.js')") as Promise<dynamic>).await()

    PixiJsAddon(
        pixiModule = module.default ?: module,
    )
}

/** Accessor for the pixi.js addon on the registry. */
val AddonRegistry.pixiJs: Addon<PixiJsAddon>
    get() = this[pixiJsAddonKey]
