package de.peekandpoke.kraft.routing

import de.peekandpoke.kraft.KraftApp
import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.components.getAttributeRecursive
import de.peekandpoke.kraft.routing.Router.RouterStrategy.Companion.HASH_PREFIX
import de.peekandpoke.ultra.common.TypedKey
import de.peekandpoke.ultra.common.isUrlWithProtocol
import de.peekandpoke.ultra.streams.Stream
import de.peekandpoke.ultra.streams.StreamSource
import kotlinx.browser.window
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.Node
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventTarget
import org.w3c.dom.events.MouseEvent

/**
 * The Router
 */
class Router(
    private val mountedRoutes: List<MountedRoute>,
    strategyProvider: (Router) -> RouterStrategy,
    private var enabled: Boolean,
) {
    companion object {
        val key = TypedKey<Router>("router")

        @RouterDsl
        val KraftApp.router: Router get() = appAttributes[key]!!

        @RouterDsl
        val Component<*>.router get() = getAttributeRecursive(key)

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

    interface RouterStrategy : Route.Renderer {
        companion object {
            const val HASH_PREFIX = "#"
        }

        fun init()

        fun navigateToUri(uri: String)

        fun replaceUri(uri: String)

        fun getUriFromWindowLocation(): String
    }

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

    class PathRoutingStrategy(private val router: Router) : RouterStrategy {
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
            if (router.enabled) {
//                console.log("popstateListener: ${event.type}", event)
                event.preventDefault()
                router.navigateToWindowUri()
            }
        }

        /**
         * Private listener for click events to intercept navigation (history-based routing)
         */
        @Suppress("detekt:ReturnCount")
        private fun clickListener(event: Event) {
//            console.log("clickListener: ${event.type}", event)

            if (!router.enabled) return

//            console.log("clickListener: 1")

            val mouseEvent = event as? MouseEvent ?: return

//            console.log("clickListener: 2", mouseEvent.target)

            // Find the closest anchor element by traversing up the DOM tree
            val target = findClosestAnchor(mouseEvent.target) ?: return

//            console.log("clickListener: 3")

            // Check if it's an internal link
            val href = target.getAttribute("href") ?: return

//            console.log("clickListener: 4")

            // External link, let browser handle it
            if (href.isUrlWithProtocol()) return

//            console.log("clickListener: 5")

            // Check if we should open in new tab -> let browser handle it
            if (willOpenNewTab(mouseEvent)) return

//            console.log("clickListener: 6")

            event.preventDefault()
            router.navToUri(uri = href)
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
                if (current is org.w3c.dom.Document) {
                    break
                }
            }

            return null
        }
    }

    data class History(
        val router: Router,
        val entries: List<ActiveRoute>,
    ) {
        val canGoBack: Boolean = entries.size > 1

        fun navBack() {
            router.navBack()
        }
    }

    val strategy: RouterStrategy = strategyProvider(this)

    /** Writable stream with the current [ActiveRoute] */
    private val _current = StreamSource(ActiveRoute("", Route.Match.default, MountedRoute.default))
    val current: Stream<ActiveRoute> = _current

    /** Writable stream with the history of [ActiveRoute]s */
    private val _historyEntries: MutableList<ActiveRoute> = mutableListOf()
    private val _history = StreamSource(History(this, _historyEntries.toList()))
    val history: Stream<History> = _history

    init {
        strategy.init()

        current.subscribeToStream {
            _historyEntries.add(it)
            _history(History(this, _historyEntries.toList()))
        }
    }

    fun disable() {
        enabled = false
    }

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

        // Go through all routes and try to find the first one that matches the current location
        return mountedRoutes.asSequence()
            // Find matches
            .map { mounted -> mounted.route.match(uri)?.let { mounted to it } }
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
