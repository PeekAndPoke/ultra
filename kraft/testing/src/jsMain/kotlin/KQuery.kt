package io.peekandpoke.kraft.testing

import io.peekandpoke.kraft.testing.KQuery.Selector.Companion.cssSelector
import org.w3c.dom.Element
import org.w3c.dom.asList

/**
 * Lightweight DOM query wrapper for test assertions.
 *
 * Wraps a list of DOM [Element]s and provides CSS-selector-based querying via [select] and [selectCss].
 * Implements [List] by delegation so standard collection operations work directly.
 */
class KQuery<E : Element>(private val elements: List<E>) : List<E> by elements {

    /** Strategy for selecting elements from a [KQuery]. */
    interface Selector<T : Element> {

        companion object {
            /** Converts a CSS selector string to a [Selector] returning generic [Element]s. */
            val String.cssSelector get() = Css(this) { filterIsInstance<Element>() }

            /** Converts a CSS selector string to a [Selector] returning elements of type [T]. */
            inline fun <reified T : Element> String.cssSelector() = Css(this) { filterIsInstance<T>() }
        }

        /** Applies this selector to the given [input] query and returns matching elements. */
        suspend fun <E : Element> applyTo(input: KQuery<E>): KQuery<T>

        /** A [Selector] that uses `querySelectorAll` with a CSS selector string. */
        class Css<T : Element>(
            private val selector: String,
            private val filter: List<Any>.() -> List<T>,
        ) : Selector<T> {

            override suspend fun <E : Element> applyTo(input: KQuery<E>): KQuery<T> {
                val found = input.elements
                    .flatMap { it.querySelectorAll(selector).asList().filter() }
                    .distinct()

                return KQuery(elements = found)
            }
        }
    }

    /** Queries the current elements using the given [selector] and returns a new [KQuery] of matches. */
    suspend fun <T : Element> select(selector: Selector<T>): KQuery<T> {
        return selector.applyTo(this)
    }

    /** Queries the current elements using a CSS selector string. */
    suspend fun selectCss(css: String): KQuery<Element> {
        return select(css.cssSelector)
    }

    /** Queries the current elements using a CSS selector string, filtering to type [T]. */
    suspend inline fun <reified T : Element> selectCss(css: String = "*"): KQuery<T> {
        return select(css.cssSelector<T>())
    }
}
