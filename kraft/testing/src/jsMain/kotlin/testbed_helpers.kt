package io.peekandpoke.kraft.testing

import org.w3c.dom.Element
import org.w3c.dom.HTMLElement

/** Simulates a click on all HTML elements in this query. Returns this query for chaining. */
fun <E : Element> KQuery<E>.click(): KQuery<E> = apply {
    filterIsInstance<HTMLElement>()
        .forEach { it.click() }
}

/** Returns the concatenated text content of all elements, joined by [glue]. */
fun <E : Element> KQuery<E>.textContent(glue: String = ""): String {
    return mapNotNull { it.textContent }
        .joinToString(glue) { it }
}

/** Returns true if any element's text content contains the given [text]. */
fun <E : Element> KQuery<E>.containsText(text: String): Boolean {
    return any { it.textContent?.contains(text) ?: false }
}
