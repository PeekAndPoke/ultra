package io.peekandpoke.kraft.addons.nxcompile

import io.peekandpoke.kraft.KraftDsl
import io.peekandpoke.kraft.addons.registry.Addon
import io.peekandpoke.kraft.addons.registry.AddonKey
import io.peekandpoke.kraft.addons.registry.AddonRegistry
import io.peekandpoke.kraft.addons.registry.AddonRegistryBuilder
import kotlinx.coroutines.await
import kotlin.js.Promise

/**
 * Facade for the lazily-loaded @nx-js/compiler-util addon.
 *
 * Provides type-safe access to sandboxed JavaScript code evaluation.
 *
 * See: https://blog.risingstack.com/writing-a-javascript-framework-sandboxed-code-evaluation/
 */
class NxCompileAddon internal constructor(
    private val compileCodeFn: dynamic,
) {
    /** Compiles the given [code] and returns a callable function. */
    fun compileCode(code: String): dynamic {
        return compileCodeFn(code)
    }
}

/** Key for the nxcompile addon. */
val nxCompileAddonKey = AddonKey<NxCompileAddon>("nxcompile")

/** Registers the nxcompile addon for lazy loading via dynamic import. */
@KraftDsl
fun AddonRegistryBuilder.nxCompile(lazy: Boolean = false): Addon<NxCompileAddon> = register(
    key = nxCompileAddonKey,
    name = "nxcompile",
    lazy = lazy,
) {
    @Suppress("UnsafeCastFromDynamic")
    val module: dynamic = (js("import('@nx-js/compiler-util')") as Promise<dynamic>).await()

    NxCompileAddon(
        compileCodeFn = module.compileCode,
    )
}

/** Accessor for the nxcompile addon on the registry. */
val AddonRegistry.nxCompile: Addon<NxCompileAddon>
    get() = this[nxCompileAddonKey]
