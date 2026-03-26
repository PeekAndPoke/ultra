package io.peekandpoke.kraft.addons.avatars

import io.peekandpoke.kraft.addons.avatars.js.JsMinIdenticons
import kotlin.random.Random

/** Entry point for avatar generation utilities. */
object Avatars {

    /** Generates SVG identicon avatars using the minidenticons library. */
    object MinIdenticon {

        /** Generates an SVG identicon string for [name] with optional [saturation] and [lightness]. */
        fun get(name: String, saturation: Number? = null, lightness: Number? = null): String {
//            console.log(JsMinIdenticons)

            return JsMinIdenticons.minidenticon(
                name = name,
                saturation = saturation ?: undefined,
                lightness = lightness ?: undefined,
            )
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

        /** Generates a random identicon as a `data:image/svg+xml` URL. */
        fun getRandomDataUrl(saturation: Number? = null, lightness: Number? = null): String {
            return getRandom(saturation = saturation, lightness = lightness).asDataUrl()
        }

        /** Generates an identicon for [name] as a `data:image/svg+xml` URL. */
        fun getDataUrl(name: String, saturation: Number? = null, lightness: Number? = null): String {
            return get(name, saturation, lightness).asDataUrl()
        }

        private fun String.asDataUrl(): String {
            return "data:image/svg+xml;utf8,$this"
        }
    }
}
