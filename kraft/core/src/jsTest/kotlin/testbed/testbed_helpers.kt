package de.peekandpoke.kraft.testbed

import org.w3c.dom.Element
import org.w3c.dom.HTMLElement

fun <E : Element> KQuery<E>.click(): KQuery<E> = apply {
    filterIsInstance<HTMLElement>()
        .forEach { it.click() }
}

fun <E : Element> KQuery<E>.textContent(glue: String = ""): String {
    return mapNotNull { it.textContent }
        .joinToString(glue) { it }
}

fun <E : Element> KQuery<E>.containsText(text: String): Boolean {
    return any { it.textContent?.contains(text) ?: false }
}
