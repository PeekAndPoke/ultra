package de.peekandpoke.kraft

import de.peekandpoke.kraft.components.AutoMountedUi
import de.peekandpoke.kraft.modals.modals
import de.peekandpoke.kraft.popups.popups
import de.peekandpoke.kraft.routing.RootRouterBuilder
import de.peekandpoke.kraft.routing.Router
import de.peekandpoke.kraft.routing.RouterDsl
import de.peekandpoke.kraft.toasts.toasts
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
        private val router = RootRouterBuilder()

        init {
            // We always add the default Modals manager
            modals()
            // We always add the default toasts manager
            toasts()
            // We always add the default Popups manager
            popups()
            // We always have the default responsive controller
            responsive(ResponsiveController())
        }

        @RouterDsl
        fun <T> setAttribute(key: TypedKey<T>, value: T) = apply {
            appAttributes[key] = value
        }

        @RouterDsl
        fun routing(block: RootRouterBuilder.() -> Unit) = apply {
            router.block()
        }

        @RouterDsl
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

    private val autoMountedUis: List<AutoMountedUi> = run {
        appAttributes.entries
            .mapNotNull { (_, v) -> v as? AutoMountedUi }
            .sortedByDescending { it.priority }
    }

    fun mount(selector: String, engine: VDomEngine, view: VDom.() -> Any?) {
        val element = document.querySelector(selector) as? HTMLElement
            ?: error("No element found for selector '$selector'")

        mount(element, engine, view)
    }

    fun mount(element: HTMLElement, engine: VDomEngine, view: VDom.() -> Any?) {
        engine.mount(app = this, element = element) {
            autoMountedUis.forEach { it.mount(this) }
            view()
        }

        // Navigate to the current URI, if there is a router present
        appAttributes[Router.key]?.navigateToWindowUri()
    }
}

