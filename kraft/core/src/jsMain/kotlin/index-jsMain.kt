package de.peekandpoke.kraft

import de.peekandpoke.kraft.components.Automount
import de.peekandpoke.kraft.vdom.VDom
import de.peekandpoke.kraft.vdom.VDomEngine
import de.peekandpoke.ultra.common.MutableTypedAttributes
import de.peekandpoke.ultra.common.TypedAttributes
import de.peekandpoke.ultra.common.TypedKey
import de.peekandpoke.ultra.common.datetime.kotlinx.initializeJsJodaTimezones
import org.w3c.dom.HTMLElement

@DslMarker
annotation class KraftDsl

fun kraftApp(block: KraftApp.Builder.() -> Unit = {}) = KraftApp.Builder().apply(block).build()

class KraftApp internal constructor(
    val appAttributes: TypedAttributes,
) {
    class Builder internal constructor() {
        private val attributes = MutableTypedAttributes.empty()

        fun <T> setAttribute(key: TypedKey<T>, value: T) = apply {
            attributes[key] = value
        }


        internal fun build() = KraftApp(
            appAttributes = attributes.asImmutable(),
        )
    }

    init {
        initializeJsJodaTimezones()
    }

    private val automounted: List<Automount> = run {
        appAttributes.entries
            .mapNotNull { (_, v) -> v as? Automount }
            .sortedByDescending { it.priority }
    }

    fun mount(element: HTMLElement, engine: VDomEngine, view: VDom.() -> Any?) {
        engine.mount(app = this, element = element) {
            automounted.forEach { it.mount(this) }
            view()
        }
    }

//    fun mount(tag: FlowContent, block: FlowContent.() -> Unit) {
//        with(tag) {
//            automounted.forEach { it.mount(this) }
//            block()
//        }
//    }
}
