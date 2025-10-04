package de.peekandpoke.kraft

import de.peekandpoke.kraft.components.Automount
import de.peekandpoke.kraft.routing.Router
import de.peekandpoke.kraft.routing.RouterBuilder
import de.peekandpoke.kraft.utils.ResponsiveController
import de.peekandpoke.kraft.vdom.VDom
import de.peekandpoke.kraft.vdom.VDomEngine
import de.peekandpoke.ultra.common.MutableTypedAttributes
import de.peekandpoke.ultra.common.TypedAttributes
import de.peekandpoke.ultra.common.TypedKey
import de.peekandpoke.ultra.common.datetime.kotlinx.initializeJsJodaTimezones
import kotlinx.browser.document
import org.w3c.dom.HTMLElement

@DslMarker
annotation class KraftDsl

@KraftDsl
fun kraftApp(block: KraftApp.Builder.() -> Unit = {}) = KraftApp.Builder().apply(block).build()

class KraftApp internal constructor(
    val appAttributes: TypedAttributes,
) {
    @KraftDsl
    class Builder internal constructor() {
        private val appAttributes = MutableTypedAttributes.empty()
        private val router = RouterBuilder()

        init {
            appAttributes[ResponsiveController.key] = ResponsiveController()
        }

        fun <T> setAttribute(key: TypedKey<T>, value: T) = apply {
            appAttributes[key] = value
        }

        fun routing(block: RouterBuilder.() -> Unit) = apply {
            router.block()
        }

        fun responsive(ctrl: ResponsiveController) = setAttribute(ResponsiveController.key, ctrl)

        internal fun build(): KraftApp {
            appAttributes[Router.key] = router.build()

            return KraftApp(
                appAttributes = appAttributes.asImmutable(),
            )
        }
    }

    init {
        initializeJsJodaTimezones()
    }

    private val automounted: List<Automount> = run {
        appAttributes.entries
            .mapNotNull { (_, v) -> v as? Automount }
            .sortedByDescending { it.priority }
    }

    fun mount(selector: String, engine: VDomEngine, view: VDom.() -> Any?) {
        val element = document.querySelector(selector) as? HTMLElement
            ?: error("No element found for selector '$selector'")

        mount(element, engine, view)
    }

    fun mount(element: HTMLElement, engine: VDomEngine, view: VDom.() -> Any?) {
        engine.mount(app = this, element = element) {
            automounted.forEach { it.mount(this) }
            view()
        }

        // Navigate to the current URI, if there is a router present
        appAttributes[Router.key]?.navigateToWindowUri()
    }
}
