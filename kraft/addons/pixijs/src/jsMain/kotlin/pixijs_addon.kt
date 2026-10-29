package io.peekandpoke.kraft.addons.pixijs

import io.peekandpoke.kraft.addons.pixijs.js.Application
import io.peekandpoke.kraft.addons.pixijs.js.Container
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
    /** Raw module handle — escape hatch for APIs not covered by the curated externals. */
    val raw: dynamic get() = pixiModule

    /** Creates a new, empty [Container]. */
    fun createContainer(): Container = create("Container")

    /**
     * A new instance of pixi's class [className], made with [args]: for a class the addon has no constructor for.
     * [T] is not checked; fails with the name when pixi has no such class.
     */
    fun <T> create(className: String, vararg args: Any?): T {
        val ctor: dynamic = pixiModule[className]

        require(ctor != null && ctor != undefined) { "pixi.js has no class '$className'" }

        return js("Reflect").construct(ctor, args).unsafeCast<T>()
    }

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
    @Suppress("unused")
    fun createText(options: TextOptions): Text {
        @Suppress("UnusedVariable", "unused")
        val ctor = pixiModule.Text
        return js("new ctor(options)").unsafeCast<Text>()
    }
}

/** Key for the pixi.js addon. */
val pixiJsAddonKey = AddonKey<PixiJsAddon>("pixijs")

/** Registers the pixi.js addon for lazy loading via dynamic import. */
fun AddonRegistryBuilder.pixiJs(lazy: Boolean = false): Addon<PixiJsAddon> = register(
    key = pixiJsAddonKey,
    name = "pixijs",
    lazy = lazy,
) {
    @Suppress("UnsafeCastFromDynamic")
    val module: dynamic = (js("import('pixi.js')") as Promise<dynamic>).await()
    val pixi: dynamic = module.default ?: module

    registerExtensions(pixi)

    PixiJsAddon(pixiModule = pixi)
}

/**
 * Makes sure pixi's extensions are registered, whatever the bundler did.
 *
 * Nine pixi classes register their extensions through a side-effect-only `import './init.mjs'` inside the class module
 * (`Application`: the ticker and resize plugins; `Graphics`, `Text` and the other scene classes: their render pipes and
 * systems). Webpack 5.108 drops those imports, so `init()` resolves but `app.ticker` is undefined and a `Graphics`
 * renders nothing. Two remedies: import every such init entry (a dynamic import always runs), and register the two
 * Application plugins explicitly as well (pixi skips extensions already registered).
 */
private suspend fun registerExtensions(pixi: dynamic) {
    // Literal specifiers, so the bundler can resolve each one.
    listOf<Promise<dynamic>>(
        js("import('pixi.js/app')"),
        js("import('pixi.js/graphics')"),
        js("import('pixi.js/text')"),
        js("import('pixi.js/text-bitmap')"),
        js("import('pixi.js/text-html')"),
        js("import('pixi.js/mesh')"),
        js("import('pixi.js/sprite-tiling')"),
        js("import('pixi.js/sprite-nine-slice')"),
        js("import('pixi.js/particle-container')"),
    ).forEach { it.await() }

    val extensions = pixi.extensions
    if (extensions == null || extensions == undefined) return

    listOf(pixi.TickerPlugin, pixi.ResizePlugin)
        .filter { it != null && it != undefined }
        .forEach { extensions.add(it) }
}

/** Accessor for the pixi.js addon on the registry. */
val AddonRegistry.pixiJs: Addon<PixiJsAddon>
    get() = this[pixiJsAddonKey]
