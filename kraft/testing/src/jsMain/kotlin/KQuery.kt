package io.peekandpoke.kraft.testing

import org.w3c.dom.Element
import org.w3c.dom.asList

/**
 * Lightweight DOM query wrapper for test assertions.
 *
 * Wraps a list of DOM [Element]s and provides querying via extension functions in kquery_select.kt.
 * Implements [List] by delegation so standard collection operations work directly.
 */
class KQuery<out E : Element>(internal val elements: List<E>) : List<E> by elements {

    /** Strategy for selecting elements from a [KQuery]. */
    interface Selector<out T : Element> {

        companion object {
            /** Converts a CSS selector string to a [Selector] returning generic [Element]s. */
            val String.cssSelector get() = Css(this) { filterIsInstance<Element>() }

            /** Converts a CSS selector string to a [Selector] returning elements of type [T]. */
            inline fun <reified T : Element> String.cssSelector() = Css(this) { filterIsInstance<T>() }
        }

        /** Applies this selector to the given [input] query and returns matching elements. */
        suspend fun applyTo(input: KQuery<Element>): KQuery<T>

        /** A [Selector] that uses `querySelectorAll` with a CSS selector string. */
        class Css<out T : Element>(
            private val selector: String,
            private val filter: List<Any>.() -> List<T>,
        ) : Selector<T> {

            override suspend fun applyTo(input: KQuery<Element>): KQuery<T> {
                val found = input.elements
                    .flatMap { it.querySelectorAll(selector).asList().filter() }
                    .distinct()

                return KQuery(elements = found)
            }
        }
    }
}
