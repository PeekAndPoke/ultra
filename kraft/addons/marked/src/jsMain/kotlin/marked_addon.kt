package io.peekandpoke.kraft.addons.marked

import io.peekandpoke.kraft.KraftDsl
import io.peekandpoke.kraft.addons.registry.Addon
import io.peekandpoke.kraft.addons.registry.AddonKey
import io.peekandpoke.kraft.addons.registry.AddonRegistry
import io.peekandpoke.kraft.addons.registry.AddonRegistryBuilder
import kotlinx.coroutines.await
import kotlin.js.Promise

/**
 * Facade for the lazily-loaded marked + dompurify addon.
 *
 * Provides type-safe access to markdown-to-HTML conversion with XSS sanitization.
 */
class MarkedAddon internal constructor(
    private val markedModule: dynamic,
    private val dompurifyModule: dynamic,
) {
    /** Parses [markdown] to sanitized HTML. */
    fun markdown2html(markdown: String): String {
        val dirty: String = markedModule.parse(markdown) as String
        val clean: String = dompurifyModule.sanitize(dirty) as String
        return clean
    }

    /** Parses [markdown] to sanitized HTML with custom purify [options]. */
    fun markdown2html(markdown: String, options: dynamic): String {
        val dirty: String = markedModule.parse(markdown) as String
        val clean: String = dompurifyModule.sanitize(dirty, options) as String
        return clean
    }

    /** Configures marked with the given [settings]. */
    fun use(settings: dynamic) {
        markedModule.use(settings)
    }
}

/** Key for the marked addon. */
val markedAddonKey = AddonKey<MarkedAddon>("marked")

/** Registers the marked addon for lazy loading via dynamic import. */
@KraftDsl
fun AddonRegistryBuilder.marked(lazy: Boolean = false): Addon<MarkedAddon> = register(
    key = markedAddonKey,
    name = "marked",
    lazy = lazy,
) {
    @Suppress("UnsafeCastFromDynamic")
    val markedModule: dynamic = (js("import('marked')") as Promise<dynamic>).await()

    @Suppress("UnsafeCastFromDynamic")
    val dompurifyModule: dynamic = (js("import('dompurify')") as Promise<dynamic>).await()

    MarkedAddon(
        markedModule = markedModule.marked ?: markedModule.default ?: markedModule,
        dompurifyModule = dompurifyModule.default ?: dompurifyModule,
    )
}

/** Accessor for the marked addon on the registry. */
val AddonRegistry.marked: Addon<MarkedAddon>
    get() = this[markedAddonKey]
