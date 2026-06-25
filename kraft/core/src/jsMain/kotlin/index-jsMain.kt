package io.peekandpoke.kraft

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
import io.peekandpoke.ultra.datetime.installNativeTimezones
import kotlinx.browser.document
import kotlinx.html.FlowContent
import org.w3c.dom.HTMLElement

@DslMarker
annotation class KraftDsl

fun kraftApp(block: KraftApp.Builder.() -> Unit = {}) = KraftApp.Builder().apply(block).build()

/** App-attribute key holding the timezone [KraftApp.AppInitializer], overridable via [KraftApp.Builder.timezones]. */
private val TimeZoneInitializerKey: TypedKey<KraftApp.AppInitializer> = TypedKey("kraft.timezone.initializer")

/** Default timezone support: the native browser provider via `Intl` (zero bundled data). */
private object NativeTimeZoneInitializer : KraftApp.AppInitializer {
    // Time zones must be ready before any other initializer or view code resolves a zone.
    override val initPriority: Int = Int.MAX_VALUE
    override fun initialize() = installNativeTimezones()
}

class KraftApp internal constructor(
    val appAttributes: TypedAttributes,
) {
    /**
     * A component that can be automatically mounted to the DOM.
     *
     * Set an app attribute whose value implements this interface; all such UIs are mounted by
     * [KraftApp] during `mount`, highest [autoMountPriority] first.
     */
    interface AutoMountedUi {
        /** The priority of the component. Higher values are mounted first */
        val autoMountPriority: Int get() = 0

        /** Mounts the component to the given element */
        fun autoMount(element: FlowContent)
    }

    /**
     * A one-shot startup task that runs once, before the app is bootstrapped (mounted).
     *
     * Set an app attribute whose value implements this interface (e.g. via [Builder.timezones]).
     * All initializers run in [KraftApp]'s init, highest [initPriority] first. Use it for global
     * startup side-effects such as timezone setup.
     */
    interface AppInitializer {
        /** The priority of the initializer. Higher values run first. */
        val initPriority: Int get() = 0

        /** Runs the initialization. */
        fun initialize()
    }

    @KraftDsl
    class Builder internal constructor() {
        private val appAttributes = MutableTypedAttributes.empty()
        private val router = RootRouterBuilder()

        init {
            // Native timezone support by default (zero bundled data); override with timezones(...)
            timezones(NativeTimeZoneInitializer)
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
        fun <T> setAttribute(key: TypedKey<T>, value: T) = apply {
            appAttributes[key] = value
        }

        /** Sets the responsive controller for the app. */
        fun responsive(ctrl: ResponsiveController) = setAttribute(ResponsiveController.key, ctrl)

        /** Sets the window controller for the app. */
        fun windowCtrl(ctrl: WindowController) = setAttribute(WindowController.key, ctrl)

        /**
         * Configures timezone support. Default: the native browser provider (Intl, zero data).
         * Override with a bundled dataset from `kraft:addons:datetime`, e.g. `fullTimezones()`.
         */
        fun timezones(initializer: AppInitializer) = setAttribute(TimeZoneInitializerKey, initializer)

        /** Sets the router for the app. */
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
        // Run startup initializers (e.g. timezone setup) before the app is bootstrapped.
        appAttributes.entries
            .mapNotNull { (_, v) -> v as? AppInitializer }
            .sortedByDescending { it.initPriority }
            .forEach { it.initialize() }
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
