package de.peekandpoke.kraft.testbed

import de.peekandpoke.kraft.testbed.KQuery.Selector.Companion.cssSelector
import org.w3c.dom.Element
import org.w3c.dom.asList

class KQuery<E : Element>(private val elements: List<E>) : List<E> by elements {

    interface Selector<T : Element> {

        companion object {
            val String.cssSelector get() = Css(this) { filterIsInstance<Element>() }

            inline fun <reified T : Element> String.cssSelector() = Css(this) { filterIsInstance<T>() }
        }

        suspend fun <E : Element> applyTo(input: KQuery<E>): KQuery<T>

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

    suspend fun <T : Element> select(selector: Selector<T>): KQuery<T> {
        return selector.applyTo(this)
    }

    suspend fun selectCss(css: String): KQuery<Element> {
        return select(css.cssSelector)
    }

    suspend inline fun <reified T : Element> selectCss(css: String = "*"): KQuery<T> {
        return select(css.cssSelector<T>())
    }
}

