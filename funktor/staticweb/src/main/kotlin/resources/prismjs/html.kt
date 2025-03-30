package de.peekandpoke.ktorfx.staticweb.resources.prismjs

import kotlinx.html.FlowContent
import kotlinx.html.code
import kotlinx.html.pre

/**
 * Renders markup for prism js with the given [language]
 */
fun FlowContent.prism(language: String, code: () -> String) {
    pre {
        code(classes = "highlight language-${language.lowercase()}") {
            +code()
        }
    }
}

/**
 * Renders markup for prism js with the given [language]
 */
fun FlowContent.prism(language: Language, code: () -> String) = prism(language.toString(), code)
