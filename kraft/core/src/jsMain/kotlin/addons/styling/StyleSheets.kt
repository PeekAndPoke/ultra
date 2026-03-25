package io.peekandpoke.kraft.addons.styling

import kotlinx.browser.document
import kotlin.random.Random

object StyleSheets {

    private val mangleMap = mutableMapOf<String, MutableSet<String>>()

    private val random = Random(1)

    fun mount(style: StyleSheetDefinition) {
        document.head?.let {
            style.mount(it)
        }
    }

    fun unmount(style: StyleSheetDefinition) {
        style.unmount()
    }

    internal fun mangleClassName(cls: String): String {
        val set = mangleMap.getOrPut(cls) { mutableSetOf() }

        val chars = "abcdefghijklmnopqrstuvwxyzABCDEFGHJKLMNPQRSTUVWXYZ0123456789"
        val numChars = chars.length

        lateinit var mangled: String

        do {
            mangled = cls + "_" + (0..7).joinToString("") {
                chars[random.nextInt(until = numChars)].toString()
            }
        } while (mangled in set)

        set.add(mangled)

        return mangled
    }
}
