package io.peekandpoke.kraft.addons.sourcemappedstacktrace

import io.peekandpoke.kraft.KraftDsl
import io.peekandpoke.kraft.addons.registry.Addon
import io.peekandpoke.kraft.addons.registry.AddonKey
import io.peekandpoke.kraft.addons.registry.AddonRegistry
import io.peekandpoke.kraft.addons.registry.AddonRegistryBuilder
import io.peekandpoke.kraft.utils.js
import kotlinx.coroutines.await
import kotlin.js.Promise

/**
 * Facade for the lazily-loaded sourcemapped-stacktrace addon.
 *
 * Provides type-safe access to stack trace source-mapping.
 */
class SourceMappedStackTraceAddon internal constructor(
    private val mapStackTraceFn: dynamic,
) {
    /** Maps a minified [stack] trace back to original source locations, calling [done] with the result. */
    fun mapStackTrace(stack: String, done: (Array<String>) -> Unit, options: dynamic): dynamic {
        return mapStackTraceFn(stack, done, options)
    }

    /** Maps a minified [stack] trace to original sources with global caching enabled. */
    fun mapStackTraceCached(stack: String, done: (Array<String>) -> dynamic): dynamic {
        return mapStackTrace(
            stack,
            done,
            mapOf(
                "cacheGlobally" to true
            ).js
        )
    }
}

/** Key for the sourcemapped-stacktrace addon. */
val sourceMappedStackTraceAddonKey = AddonKey<SourceMappedStackTraceAddon>("sourcemappedstacktrace")

/** Registers the sourcemapped-stacktrace addon for lazy loading via dynamic import. */
@KraftDsl
fun AddonRegistryBuilder.sourceMappedStackTrace(lazy: Boolean = false): Addon<SourceMappedStackTraceAddon> = register(
    key = sourceMappedStackTraceAddonKey,
    name = "sourcemappedstacktrace",
    lazy = lazy,
) {
    @Suppress("UnsafeCastFromDynamic")
    val module: dynamic = (js("import('sourcemapped-stacktrace')") as Promise<dynamic>).await()

    SourceMappedStackTraceAddon(
        mapStackTraceFn = module.mapStackTrace,
    )
}

/** Accessor for the sourcemapped-stacktrace addon on the registry. */
val AddonRegistry.sourceMappedStackTrace: Addon<SourceMappedStackTraceAddon>
    get() = this[sourceMappedStackTraceAddonKey]
