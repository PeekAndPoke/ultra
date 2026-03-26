package io.peekandpoke.kraft.addons.styling

import kotlinx.browser.document
import kotlin.random.Random

/**
 * Global registry for mounting and unmounting [StyleSheetDefinition] instances into the document head.
 */
object StyleSheets {

    private val mangleMap = mutableMapOf<String, MutableSet<String>>()

    private val random = Random(1)

    /** Mounts the given [style] sheet into the document head. */
    fun mount(style: StyleSheetDefinition) {
        document.head?.let {
            style.mount(it)
        }
    }

    /** Unmounts the given [style] sheet from the DOM. */
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
