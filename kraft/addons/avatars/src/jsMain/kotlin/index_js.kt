package de.peekandpoke.kraft.addons.avatars

import de.peekandpoke.kraft.addons.avatars.js.JsMinIdenticons
import kotlin.random.Random

object Avatars {

    object MinIdenticon {

        fun get(name: String, saturation: Number? = null, lightness: Number? = null): String {
//            console.log(JsMinIdenticons)

            return JsMinIdenticons.minidenticon(
                name = name,
                saturation = saturation ?: undefined,
                lightness = lightness ?: undefined,
            )
        }

        fun getRandom(saturation: Number? = null, lightness: Number? = null): String {
            val name = (0..16).joinToString { Random.nextInt().toString() }

            return get(
                name = name,
                saturation = saturation,
                lightness = lightness,
            )
        }

        fun getRandomDataUrl(saturation: Number? = null, lightness: Number? = null): String {
            return getRandom(saturation = saturation, lightness = lightness).asDataUrl()
        }

        fun getDataUrl(name: String, saturation: Number? = null, lightness: Number? = null): String {
            return get(name, saturation, lightness).asDataUrl()
        }

        private fun String.asDataUrl(): String {
            return "data:image/svg+xml;utf8,$this"
        }
    }
}
