package de.peekandpoke.kraft.addons.styling

import kotlinx.css.CssBuilder
import org.w3c.dom.HTMLElement
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadOnlyProperty

abstract class StyleSheet(name: String? = null) : StyleSheetDefinition {

    val css: String get() = builder.toString()

    private var mounter = RawCssMounter(name = name) { css }
    private val builder = CssBuilder()

    override fun mount(into: HTMLElement): Unit = mounter.mount(into)

    override fun unmount(): Unit = mounter.unmount()

    fun rule(
        block: CssBuilder.() -> Unit,
    ): PropertyDelegateProvider<Any?, ReadOnlyProperty<Any?, CssRule>> {
        return PropertyDelegateProvider { _, property ->
            val mangled = makeRule(property.name, null, block)
            val ruleObj = CssRule(mangled)
            ReadOnlyProperty { _, _ -> ruleObj }
        }
    }

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
