package io.peekandpoke.kraft.addons.avatars

import io.peekandpoke.kraft.KraftDsl
import io.peekandpoke.kraft.addons.registry.Addon
import io.peekandpoke.kraft.addons.registry.AddonKey
import io.peekandpoke.kraft.addons.registry.AddonRegistry
import io.peekandpoke.kraft.addons.registry.AddonRegistryBuilder
import kotlinx.coroutines.await
import kotlin.js.Promise
import kotlin.random.Random

/**
 * Facade for the lazily-loaded minidenticons addon.
 *
 * Provides type-safe access to SVG identicon avatar generation.
 */
class AvatarsAddon internal constructor(
    private val minidenticonsModule: dynamic,
) {
    /** Generates an SVG identicon string for [name] with optional [saturation] and [lightness]. */
    fun get(name: String, saturation: Number? = null, lightness: Number? = null): String {
        val minidenticonFn = minidenticonsModule.minidenticon

        return minidenticonFn(name, saturation ?: undefined, lightness ?: undefined) as String
    }

    /** Generates an SVG identicon from a random name. */
    fun getRandom(saturation: Number? = null, lightness: Number? = null): String {
        val name = (0..16).joinToString { Random.nextInt().toString() }

        return get(
            name = name,
            saturation = saturation,
            lightness = lightness,
        )
    }

    /** Generates an identicon for [name] as a `data:image/svg+xml` URL. */
    fun getDataUrl(name: String, saturation: Number? = null, lightness: Number? = null): String {
        return get(name, saturation, lightness).asDataUrl()
    }

    /** Generates a random identicon as a `data:image/svg+xml` URL. */
    fun getRandomDataUrl(saturation: Number? = null, lightness: Number? = null): String {
        return getRandom(saturation = saturation, lightness = lightness).asDataUrl()
    }

    private fun String.asDataUrl(): String {
        return "data:image/svg+xml;utf8,$this"
    }
}

/** Key for the avatars addon. */
val avatarsAddonKey = AddonKey<AvatarsAddon>("avatars")

/** Registers the avatars addon for lazy loading via dynamic import. */
@KraftDsl
fun AddonRegistryBuilder.avatars(lazy: Boolean = false): Addon<AvatarsAddon> = register(
    key = avatarsAddonKey,
    name = "avatars",
    lazy = lazy,
) {
    @Suppress("UnsafeCastFromDynamic")
    val minidenticonsModule: dynamic = (js("import('minidenticons')") as Promise<dynamic>).await()

    AvatarsAddon(
        minidenticonsModule = minidenticonsModule,
    )
}

/** Accessor for the avatars addon on the registry. */
val AddonRegistry.avatars: Addon<AvatarsAddon>
    get() = this[avatarsAddonKey]
