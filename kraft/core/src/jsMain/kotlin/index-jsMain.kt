package io.peekandpoke.kraft

import io.peekandpoke.kraft.dnd.dnd
import io.peekandpoke.kraft.i18n.I18nController
import io.peekandpoke.kraft.modals.modals
import io.peekandpoke.kraft.popups.popups
import io.peekandpoke.kraft.routing.RootRouterBuilder
import io.peekandpoke.kraft.routing.Router
import io.peekandpoke.kraft.toasts.toasts
import io.peekandpoke.kraft.utils.ResponsiveController
import io.peekandpoke.kraft.utils.WakeLockController
import io.peekandpoke.kraft.utils.WindowController
import io.peekandpoke.kraft.vdom.VDom
import io.peekandpoke.kraft.vdom.VDomEngine
import io.peekandpoke.ultra.common.MutableTypedAttributes
import io.peekandpoke.ultra.common.TypedAttributes
import io.peekandpoke.ultra.common.TypedKey
import kotlinx.browser.document
import kotlinx.html.FlowContent
import org.w3c.dom.HTMLElement

@DslMarker
annotation class KraftDsl

fun kraftApp(block: KraftApp.Builder.() -> Unit = {}) = KraftApp.Builder().apply(block).build()

/** App-attribute key holding the timezone [KraftApp.AppInitializer], overridable via [KraftApp.Builder.timezones]. */
private val TimeZoneInitializerKey: TypedKey<KraftApp.AppInitializer> = TypedKey("kraft.timezone.initializer")

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

    /** An app attribute that holds browser resources (listeners, timers); [KraftApp.dispose] releases it. */
    interface Disposable {
        /** Releases what the attribute holds. */
        fun dispose()
    }

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
            // We always add the default drag and drop manager (it listens to the window only while a drag runs)
            dnd()
            // The default responsive controller is made in build(), only if none was set: it holds a window listener
            // We always have the default window controller
            windowCtrl(WindowController.default)
            // We always have the default wake lock controller (one lock for the page)
            wakeLockCtrl(WakeLockController.default)
            // Default English i18n with kraft's built-in form catalog, so form errors translate and
            // `by Translations` never crashes; override with i18n(...)
            i18n(I18nController.default())
        }

        /** Sets an attribute for the app. */
        fun <T> setAttribute(key: TypedKey<T>, value: T) = apply {
            appAttributes[key] = value
        }

        /** Sets the responsive controller for the app. */
        fun responsive(ctrl: ResponsiveController) = setAttribute(ResponsiveController.key, ctrl)

        /** Sets the window controller for the app. */
        fun windowCtrl(ctrl: WindowController) = setAttribute(WindowController.key, ctrl)

        /** Sets the wake lock controller for the app. */
        fun wakeLockCtrl(ctrl: WakeLockController) = setAttribute(WakeLockController.key, ctrl)

        /** Sets the i18n controller for the app. Default: English with kraft's built-in form catalog. */
        fun i18n(controller: I18nController) = setAttribute(I18nController.key, controller)

        /**
         * Loads a time zone dataset for raw kotlinx `TimeZone.of(...)`, e.g. `fullTimezones()` from
         * `kraft:addons:datetime`. Default: none — ultra's `Mp*` types need no dataset.
         */
        fun timezones(initializer: AppInitializer) = setAttribute(TimeZoneInitializerKey, initializer)

        /** Sets the router for the app. */
        fun routing(block: RootRouterBuilder.() -> Unit) = apply {
            router.block()
        }

        internal fun build(): KraftApp {
            appAttributes[Router.key] = router.build()

            // We always have a responsive controller; the default only when the app set none
            if (!appAttributes.has(ResponsiveController.key)) {
                responsive(ResponsiveController())
            }

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

    /**
     * Tears the app down after it was unmounted: the router stops listening to the browser, and every [Disposable]
     * app attribute is released. TestBed calls it after each render.
     */
    fun dispose() {
        appAttributes[Router.key]?.dispose()

        appAttributes.entries
            .mapNotNull { (_, v) -> v as? Disposable }
            .forEach { it.dispose() }
    }
}
