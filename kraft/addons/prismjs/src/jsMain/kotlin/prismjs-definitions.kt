package de.peekandpoke.kraft.addons.prismjs

import org.w3c.dom.Element

@JsModule("prismjs")
@JsNonModule
external object PrismJsDefinition {
    val languages: dynamic

    val plugins: dynamic

    val hooks: dynamic

    fun highlight(code: String, languageDef: dynamic, language: String): String

    fun highlightAllUnder(container: Element)

    fun highlightElement(element: Element)
}
