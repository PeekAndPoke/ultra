package io.peekandpoke.kraft

import io.peekandpoke.kraft.components.AutoMountedUi
import io.peekandpoke.kraft.modals.modals
import io.peekandpoke.kraft.popups.popups
import io.peekandpoke.kraft.routing.RootRouterBuilder
import io.peekandpoke.kraft.routing.Router
import io.peekandpoke.kraft.toasts.toasts
import io.peekandpoke.kraft.utils.ResponsiveController
import io.peekandpoke.kraft.utils.WindowController
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.kraft.vdom.VDomEngine
import io.peekandpoke.ultra.common.MutableTypedAttributes
import io.peekandpoke.ultra.common.TypedAttributes
import io.peekandpoke.ultra.common.TypedKey
import io.peekandpoke.ultra.datetime.kotlinx.initializeJsJodaTimezones
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
            // We always have the default window controller
            windowCtrl(WindowController())
        }

        /** Sets an attribute for the app. */
        @KraftDsl
        fun <T> setAttribute(key: TypedKey<T>, value: T) = apply {
            appAttributes[key] = value
        }

        /** Sets the responsive controller for the app. */
        @KraftDsl
        fun responsive(ctrl: ResponsiveController) = setAttribute(ResponsiveController.key, ctrl)

        /** Sets the window controller for the app. */
        @KraftDsl
        fun windowCtrl(ctrl: WindowController) = setAttribute(WindowController.key, ctrl)

        /** Sets the router for the app. */
        @KraftDsl
        fun routing(block: RootRouterBuilder.() -> Unit) = apply {
            router.block()
        }

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
            .sortedByDescending { it.autoMountPriority }
    }

    fun mount(selector: String, engine: VDomEngine, view: VDom.() -> Any?) {
        val element = document.querySelector(selector) as? HTMLElement
            ?: error("No element found for selector '$selector'")

        mount(element, engine, view)
    }

    fun mount(element: HTMLElement, engine: VDomEngine, view: VDom.() -> Any?) {
        engine.mount(app = this, element = element) {
            autoMountedUis.forEach { it.autoMount(this) }
            view()
        }

        // Navigate to the current URI, if there is a router present
        appAttributes[Router.key]?.navigateToWindowUri()
    }
}
