package io.peekandpoke.kraft.addons.prismjs

import org.w3c.dom.Element

@JsModule("prismjs")
@JsNonModule
/** External binding for the PrismJS library's core API. */
external object PrismJsDefinition {
    /** Registry of loaded language grammars. */
    val languages: dynamic

    /** Registry of loaded plugins. */
    val plugins: dynamic

    /** Hook system for extending PrismJS behavior. */
    val hooks: dynamic

    /** Highlights [code] using the given [languageDef] grammar and [language] name. */
    fun highlight(code: String, languageDef: dynamic, language: String): String

    /** Highlights all code elements under [container]. */
    fun highlightAllUnder(container: Element)

    /** Highlights a single code [element]. */
    fun highlightElement(element: Element)
}
