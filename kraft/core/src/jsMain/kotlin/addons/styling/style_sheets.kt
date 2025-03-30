package de.peekandpoke.kraft.addons.styling

import kotlinx.browser.document
import kotlinx.css.CssBuilder
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLLinkElement
import org.w3c.dom.HTMLStyleElement
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

private class RawCssMounter(private val content: () -> String) {
    private var mounted: HTMLStyleElement? = null

    fun mount(into: HTMLElement) {
        if (mounted == null) {
            val css = content()

            mounted = document.createElement("style") as HTMLStyleElement
            mounted!!.setAttribute("id", "injected-${css.hashCode()}")
            mounted!!.textContent = css

//            console.log("body", body)
//            console.log("style", styleNode)

            into.appendChild(mounted!!)
        }
    }

    fun unmount() {
        mounted?.let {
            it.remove()
            mounted = null
        }
    }
}

private class LinkTagMounter(private val link: () -> HTMLLinkElement) {

    var mounted: HTMLLinkElement? = null

    fun mount(into: HTMLElement) {
        if (mounted == null) {
            mounted = link()
            into.appendChild(mounted!!)
        }
    }

    fun unmount() {
        mounted?.let {
            it.remove()
            mounted = null
        }
    }
}

interface StyleSheetDefinition {
    fun mount(into: HTMLElement)
    fun unmount()
}

class StyleSheetTag(
    block: HTMLLinkElement.() -> Unit,
) : StyleSheetDefinition {

    private val link = (document.createElement("link") as HTMLLinkElement).apply {
        rel = "stylesheet"
        block()
    }

    private val mounter = LinkTagMounter { link }

    override fun mount(into: HTMLElement): Unit = mounter.mount(into)

    override fun unmount(): Unit = mounter.unmount()
}

data class RawStyleSheet(val css: String) : StyleSheetDefinition {
    private var mounter = RawCssMounter { css }

    override fun mount(into: HTMLElement): Unit = mounter.mount(into)

    override fun unmount(): Unit = mounter.unmount()
}

abstract class StyleSheet : StyleSheetDefinition {

    val css: String get() = builder.toString()

    private var mounter = RawCssMounter { css }
    private val builder = CssBuilder()
    private var counter = 1

    override fun mount(into: HTMLElement): Unit = mounter.mount(into)

    override fun unmount(): Unit = mounter.unmount()

    fun rule(block: CssBuilder.() -> Unit): String {
        return makeRule(null, block)
    }

    fun rule(contextSelector: String, block: CssBuilder.() -> Unit): String {
        return makeRule(contextSelector, block)
    }

    private fun makeRule(contextSelector: String? = null, block: CssBuilder.() -> Unit): String {
        val cssClassName = "r$counter"

        val mangled = StyleSheets.mangleClassName(cssClassName)

        builder.apply {
            if (contextSelector == null) {
                rule(".$mangled") {
                    block()
                }
            } else {
                rule("$contextSelector.$mangled") {
                    block()
                }
            }
        }

        return mangled
    }
}
