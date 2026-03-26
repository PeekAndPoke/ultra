package io.peekandpoke.kraft.addons.styling

import kotlinx.browser.window
import kotlinx.css.CssBuilder
import org.w3c.dom.HTMLElement
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty

/**
 * Base class for defining CSS-in-Kotlin stylesheets with mangled class names.
 *
 * When [autoMount] is true (default), the stylesheet is mounted into the document head
 * on the next animation frame — after the subclass has fully initialized its [rule] delegates.
 *
 * Usage:
 * ```
 * object MyStyles : StyleSheet() {
 *     val container by rule {
 *         display = Display.flex
 *         padding = Padding(16.px)
 *     }
 * }
 * ```
 */
abstract class StyleSheet(name: String? = null, autoMount: Boolean = true) : StyleSheetDefinition {

    private val builder = CssBuilder()
    private var mounter = RawCssMounter(name = name) { css }

    init {
        if (autoMount) {
            window.requestAnimationFrame {
                StyleSheets.mount(this)
            }
        }
    }

    /** The generated CSS string. */
    val css: String get() = builder.toString()

    override fun mount(into: HTMLElement): Unit = mounter.mount(into)

    override fun unmount(): Unit = mounter.unmount()

    /**
     * Defines a CSS rule with a mangled class name.
     *
     * Use as a delegated property:
     * ```
     * val container by rule {
     *     display = Display.flex
     * }
     * ```
     */
    fun rule(
        block: CssBuilder.() -> Unit,
    ): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, CssRule>> {
        return PropertyDelegateProvider { _, property ->
            val mangled = makeRule(property.name, null, block)
            val ruleObj = CssRule(mangled)
            ReadOnlyProperty { _, _ -> ruleObj }
        }
    }

    /**
     * Defines a CSS rule scoped under a [contextSelector].
     *
     * The generated selector will be `$contextSelector.$mangledName`.
     */
    fun rule(
        contextSelector: String,
        block: CssBuilder.() -> Unit,
    ): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, CssRule>> {
        return PropertyDelegateProvider { _, property ->
            val mangled = makeRule(property.name, contextSelector, block)
            val ruleObj = CssRule(mangled)
            ReadOnlyProperty { _, _ -> ruleObj }
        }
    }

    /**
     * Defines a CSS rule scoped under a parent [contextRule].
     *
     * The generated selector will be `${contextRule.selector}.$mangledName`.
     */
    fun rule(
        contextRule: CssRule,
        block: CssBuilder.() -> Unit,
    ): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, CssRule>> {
        return PropertyDelegateProvider { _, property ->
            val mangled = makeRule(property.name, contextRule.selector, block)
            val ruleObj = CssRule(mangled)
            ReadOnlyProperty { _, _ -> ruleObj }
        }
    }

    private fun makeRule(
        cssClassName: String,
        contextSelector: String? = null,
        block: CssBuilder.() -> Unit,
    ): String {
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
