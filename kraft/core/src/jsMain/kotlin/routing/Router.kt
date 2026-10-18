package io.peekandpoke.kraft.routing

import io.peekandpoke.kraft.KraftApp
import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.getAttributeRecursive
import io.peekandpoke.kraft.routing.Router.RouterStrategy.Companion.HASH_PREFIX
import io.peekandpoke.ultra.common.TypedKey
import io.peekandpoke.ultra.streams.Stream
import io.peekandpoke.ultra.streams.StreamSource
import kotlinx.browser.document
import kotlinx.browser.window
import org.w3c.dom.Document
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.Node
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventTarget
import org.w3c.dom.events.MouseEvent
import org.w3c.dom.url.URL

/**
 * The Router
 */
@RouterDsl
class Router(
    private val mountedRoutes: List<MountedRoute>,
    strategyProvider: (Router) -> RouterStrategy,
    private var enabled: Boolean,
) {
    companion object {
        /** Typed attribute key used to store the router on components. */
        val key = TypedKey<Router>("router")

        /** Gets the [Router] from a [KraftApp]. */
        val KraftApp.router: Router get() = appAttributes[key]!!

        /** Gets the [Router] by looking up the component tree. */
        val Component<*>.router get() = getAttributeRecursive(key)

        /** Returns true if the mouse event indicates the link should open in a new tab. */
        fun willOpenNewTab(evt: MouseEvent?): Boolean {
            return when (evt) {
                null -> false
                else -> {
                    evt.ctrlKey || // Windows, Linux
                            evt.metaKey || // MacOS
                            (evt.button == 1.toShort()) // Middle Mouse button
                }
            }
        }
    }

    /** Strategy interface that controls how the router interacts with the browser's URL. */
    interface RouterStrategy : Route.Renderer {
        companion object {
            const val HASH_PREFIX = "#"
        }

        /** Initializes the strategy by installing browser event listeners. */
        fun init()

        /** Navigates to the given [uri], updating the browser location. */
        fun navigateToUri(uri: String)

        /** Replaces the current URI in the browser history without adding a new entry. */
        fun replaceUri(uri: String)

        /** Returns the current URI from the browser's location. */
        fun getUriFromWindowLocation(): String
    }

    /** Hash-based routing strategy using the URL fragment (e.g. `#/path`). */
    class HashRoutingStrategy(private val router: Router) : RouterStrategy {

        override fun init() {
            window.addEventListener("hashchange", ::windowListener)
        }

        override fun render(bound: Route.Bound): String {
            return "#" + super.render(bound)
        }

        override fun navigateToUri(uri: String) {
            val currentUri = getUriFromWindowLocation()
            val cleanUri = uri.cleanUri()

            if (currentUri != cleanUri) {
                window.location.hash = cleanUri
            } else {
                router.navigateToWindowUri()
            }
        }

        override fun replaceUri(uri: String) {
            window.history.pushState(data = null, title = "", url = uri.cleanUri())
        }

        override fun getUriFromWindowLocation(): String {
            return window.location.hash.cleanUri()
        }

        private fun String.cleanUri(): String {
            return removePrefix(HASH_PREFIX).trim()
        }

        /**
         * Private listener for the "hashchange" event
         */
        private fun windowListener(event: Event) {
            if (router.enabled) {
                event.preventDefault()

                router.navigateToWindowUri()
            }
        }
    }

    /** Path-based routing strategy using the browser's History API. */
    class PathRoutingStrategy(private val router: Router) : RouterStrategy {
        private companion object {
            /** RFC 3986 scheme followed by a colon, at the start of an href. */
            val AbsoluteUrlSchemeRegex = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*:")
        }

        override fun init() {
            window.addEventListener("popstate", ::popstateListener)
            // Intercept clicks on links to prevent page reloads
            window.addEventListener("click", ::clickListener)
        }

        override fun navigateToUri(uri: String) {
            val cleanUri = uri.cleanUri()
            val currentUri = getUriFromWindowLocation()

            if (currentUri != cleanUri) {
                window.history.pushState(null, "", cleanUri)
            }

            router.navigateToWindowUri()
        }

        override fun replaceUri(uri: String) {
            val cleanUri = uri.cleanUri()
            window.history.replaceState(null, "", cleanUri)
            router.navigateToWindowUri()
        }

        override fun getUriFromWindowLocation(): String {
            return window.location.pathname + window.location.search + window.location.hash
        }

        private fun String.cleanUri(): String {
            return "/" + trim().trimStart('/')
        }

        /**
         * Private listener for the "popstate" event (history-based routing)
         */
        private fun popstateListener(event: Event) {
            if (!router.enabled) return

            // Same path and query as the active route: only the fragment moved (in-page anchor, or Back
            // from one), so the route stays and the browser scrolls
            val uri = getUriFromWindowLocation()
            if (uri.substringBefore('#') == router.current().uri.substringBefore('#')) return

            event.preventDefault()
            router.navigateToWindowUri()
        }

        /**
         * Private listener for click events to intercept navigation (history-based routing)
         *
         * Intercepts only plain clicks on relative links to this app; everything else is left to the browser.
         */
        @Suppress("detekt:ReturnCount")
        private fun clickListener(event: Event) {
            if (!router.enabled) return

            val mouseEvent = event as? MouseEvent ?: return

            // Someone else already handled it
            if (mouseEvent.defaultPrevented) return

            // Not a plain primary click: new tab, new window, download, ...
            if (mouseEvent.button != 0.toShort()) return
            if (mouseEvent.ctrlKey || mouseEvent.metaKey || mouseEvent.shiftKey || mouseEvent.altKey) return

            // Find the closest anchor element by traversing up the DOM tree
            val anchor = findClosestAnchor(mouseEvent.target) ?: return
            val href = anchor.getAttribute("href") ?: return

            // The author asked the browser to handle it
            if (!anchor.targetsSelf()) return
            if (anchor.hasAttribute("download")) return
            if (anchor.relTokens().contains("external")) return

            // Absolute and scheme-relative hrefs always go to the browser, even on the same origin
            if (href.isAbsoluteUrl()) return

            // Resolve like the browser does, so relative hrefs land where a new tab would
            val base = document.baseURI
            val url = try {
                URL(href, base)
            } catch (_: Throwable) {
                return
            }

            // A relative href can still leave via <base href>. Protocol and host rather than origin: blob:
            // URLs carry their creator's origin, and custom-scheme hosts (Capacitor, file://) have "null".
            if (url.protocol != window.location.protocol) return
            if (url.host != window.location.host) return

            // Fragment navigation within the current document: let the browser scroll
            if (url.href.contains('#') && url.href.substringBefore('#') == window.location.href.substringBefore('#')) {
                return
            }

            event.preventDefault()
            router.navToUri(uri = url.pathname + url.search + url.hash)
        }

        /** True when the link opens in the current browsing context, honouring `<base target>`. */
        private fun HTMLAnchorElement.targetsSelf(): Boolean {
            val target = (getAttribute("target") ?: document.querySelector("base[target]")?.getAttribute("target"))
                ?.lowercase()

            return when (target) {
                null, "", "_self" -> true
                // Both name the current context when it is not framed
                "_top" -> window.top === window
                "_parent" -> window.parent === window
                // Any other value, even whitespace, names another browsing context
                else -> false
            }
        }

        /** True for `scheme:…` and `//host…` hrefs, after the clean-up the browser's URL parser applies. */
        private fun String.isAbsoluteUrl(): Boolean {
            // C0 controls and spaces at both ends, tab / LF / CR anywhere, and '\' read as '/'
            val normalized = trim { it <= ' ' }
                .filterNot { it == '\t' || it == '\n' || it == '\r' }
                .replace('\\', '/')

            return normalized.startsWith("//") || AbsoluteUrlSchemeRegex.containsMatchIn(normalized)
        }

        /** The whitespace-separated, lowercased tokens of the `rel` attribute. */
        private fun HTMLAnchorElement.relTokens(): List<String> {
            return (getAttribute("rel") ?: "").lowercase().split(' ', '\t', '\n', '\r').filter { it.isNotEmpty() }
        }

        /**
         * Traverses up the DOM tree to find the closest anchor element
         */
        private fun findClosestAnchor(target: EventTarget?): HTMLAnchorElement? {
            var current: Node? = target as? Node

            while (current != null) {
                // Check if current node is an anchor element
                if (current is HTMLAnchorElement) {
                    return current
                }

                // Move up to parent node
                current = current.parentNode

                // Stop at document level to avoid infinite loops
                if (current is Document) {
                    break
                }
            }

            return null
        }
    }

    /** Represents the navigation history of the router. */
    data class History(
        val router: Router,
        val entries: List<ActiveRoute>,
    ) {
        /** True when there is a previous route to navigate back to. */
        val canGoBack: Boolean = entries.size > 1

        /** Navigates back to the previous route. */
        fun navBack() {
            router.navBack()
        }
    }

    /** The active routing strategy (hash or path based). */
    val strategy: RouterStrategy = strategyProvider(this)

    /** Writable stream with the current [ActiveRoute] */
    private val _current = StreamSource(ActiveRoute("", Route.Match.default, MountedRoute.default))

    /** Stream that emits the currently active route whenever navigation occurs. */
    val current: Stream<ActiveRoute> = _current

    /** Writable stream with the history of [ActiveRoute]s */
    private val _historyEntries: MutableList<ActiveRoute> = mutableListOf()
    private val _history = StreamSource(History(this, _historyEntries.toList()))

    /** Stream that emits the navigation history whenever it changes. */
    val history: Stream<History> = _history

    init {
        strategy.init()

        current.subscribeToStream {
            _historyEntries.add(it)
            _history(History(this, _historyEntries.toList()))
        }
    }

    /** Disables the router, preventing it from reacting to browser navigation events. */
    fun disable() {
        enabled = false
    }

    /** Enables the router, allowing it to react to browser navigation events. */
    fun enable() {
        enabled = true
    }

    /**
     * Navigates to the given uri.
     */
    fun navToUri(uri: String) {
        strategy.navigateToUri(uri)
    }

    /**
     * Navigates to the given [route].
     */
    fun navToUri(route: Route.Bound) {
        navToUri(evt = null, route = route)
    }

    /**
     * Navigates to the given [route].
     *
     * Takes the [evt] into account, to open a new tab in the browser:
     * - when a special key where held while clicking
     * - when the middle mouse button was clicked
     */
    fun navToUri(evt: MouseEvent?, route: Route.Bound) {
        navToUri(
            evt = evt,
            uri = strategy.render(route),
        )
    }

    /**
     * Navigate to the given [uri].
     *
     * Takes the [evt] into account, to open a new tab in the browser:
     * - when a special key where held while clicking
     * - when the middle mouse button was clicked
     */
    fun navToUri(evt: MouseEvent?, uri: String) {
        navToUri(
            uri = uri,
            newTab = willOpenNewTab(evt),
        )
    }

    /**
     * Navigate to the given [uri].
     *
     * If [newTab] is true, the uri will be opened in a new tab.
     */
    fun navToUri(uri: String, newTab: Boolean) {
        return navToUri(
            uri = uri,
            target = if (newTab) "_blank" else null,
        )
    }

    /**
     * Navigate to the given [uri].
     *
     * If [target] is null, it will navigate to the uri in the same window.
     * If [target] is not null, window.open(target = target) will be used to open a new tab / window.
     */
    fun navToUri(uri: String, target: String? = null) {
        if (target == null || target.isBlank() || target == "_self") {
            navToUri(uri)
        } else {
            window.open(url = uri, target = target)
        }
    }

    /**
     * Navigates to the [route] by filling in the given [routeParams] and [queryParams].
     */
    fun navToUri(route: Route, routeParams: Map<String, String>, queryParams: Map<String, String>) {
        val bound = Route.Bound(route = route, routeParams = routeParams, queryParams = queryParams)

        val uri = strategy.render(bound)

        navToUri(uri)
    }

    /**
     * Navigates to the [route].
     */
    fun navToUri(route: Route.Match) {
        navToUri(
            route = route.route, routeParams = route.routeParams, queryParams = route.queryParams,
        )
    }

    /**
     * Replaces the current uri with changing the history
     */
    fun replaceUri(uri: String) {
        val currentUri = strategy.getUriFromWindowLocation()

        if (currentUri != uri) {
            strategy.replaceUri(uri)
            // Remove the last from the history
            _historyEntries.removeLast()
            // Resolve the next route
            navigateToWindowUri()
        }
    }

    /**
     * Replaces the current uri without changing the browser history.
     */
    fun replaceUri(route: Route, routeParams: Map<String, String>, queryParams: Map<String, String>) {
        val bound = Route.Bound(route = route, routeParams = routeParams, queryParams = queryParams)
        val uri = strategy.render(bound)

        replaceUri(uri)
    }

    /**
     * Replaces the current uri without changing the browser history.
     */
    fun replaceUri(route: Route.Match) {
        replaceUri(
            route = route.route, routeParams = route.routeParams, queryParams = route.queryParams
        )
    }

    /**
     * Navigates to the given uri while clearing the navigation history.
     */
    fun clearHistoryAndNavTo(uri: String) {
        _historyEntries.clear()
        navToUri(uri)
    }

    /**
     * Navigates to the previous route, when this is possible.
     */
    fun navBack() {
        if (history().canGoBack) {
            // remove the current active route
            _historyEntries.removeLast()
            // remove the previous active route and go back to it
            navToUri(_historyEntries.removeLast().uri)
        }
    }

    /**
     * Navigates to the current uri of the browser
     *
     * Tries to resolve the next [ActiveRoute] by looking at the browser current location.
     *
     * If a route is resolved then the [current] stream will be updated.
     */
    fun navigateToWindowUri() {
        // get the location from the browser
        val uri = strategy.getUriFromWindowLocation()

        val resolved = resolveRouteForUri(uri)

        if (resolved == null) {
            console.error("Could not resolve route: $uri")
        }

        resolved?.let { (mounted, match) ->
            val middlewareContext = RouterMiddlewareContext(uri = uri)

            // Check all middlewares
            mounted.middlewares.forEach { middleware ->
                // Call the middleware with the context
                when (val result = middleware.middleware(middlewareContext)) {
                    is RouterMiddlewareResult.Proceed -> Unit // noop

                    is RouterMiddlewareResult.Redirect -> {
                        console.log("Middleware: Redirecting to: ${result.uri}")
                        // Yes so let's go to the redirect
                        navToUri(result.uri)
                        // Stop here
                        return@let
                    }
                }
            }

            // Notify all subscribers that the active route has changed
            _current(ActiveRoute(uri, match, mounted))
        }
    }

    /**
     * Tries to resolve the next [ActiveRoute] by looking at the browser current location.
     *
     * If a route is resolved then the [current] stream will be updated.
     */
    fun resolveRouteForUri(uri: String): Pair<MountedRoute, Route.Match>? {

        // The fragment never selects a route, and would otherwise leak into the last param
        val withoutFragment = uri.substringBefore('#')

        // Go through all routes and try to find the first one that matches the current location
        return mountedRoutes.asSequence()
            // Find matches
            .map { mounted -> mounted.route.match(withoutFragment)?.let { mounted to it } }
            // Skip all that did not match
            .filterNotNull()
            // Get the first match, if there is any
            .firstOrNull()
    }

    /**
     * Checks if there is a route for the given uri.
     */
    fun hasRouteForUri(uri: String): Boolean {
        return resolveRouteForUri(uri) != null
    }
}
