package de.peekandpoke.kraft.addons.routing

import de.peekandpoke.ultra.streams.Stream
import de.peekandpoke.ultra.streams.StreamSource
import kotlinx.browser.window
import org.w3c.dom.events.Event
import org.w3c.dom.events.MouseEvent

/**
 * The Router
 */
class Router(private val mountedRoutes: List<MountedRoute>, private var enabled: Boolean) {

    data class History(
        val router: Router,
        val entries: List<ActiveRoute>,
    ) {
        val canGoBack: Boolean = entries.size > 1

        fun navBack() {
            router.navBack()
        }
    }

    private val prefix = "#"

    /** Writable stream with the current [ActiveRoute] */
    private val _current = StreamSource(ActiveRoute("", Route.Match.default, MountedRoute.default))

    /** Public readonly stream with the current [ActiveRoute] */
    val current: Stream<ActiveRoute> = _current

    private val _historyEntries: MutableList<ActiveRoute> = mutableListOf()

    private val _history = StreamSource(History(this, _historyEntries.toList()))
    val history: Stream<History> = _history

    init {
        window.addEventListener("hashchange", ::windowListener)

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

    fun willOpenNewTab(evt: MouseEvent?): Boolean {
        return when (evt) {
            null -> false
            else -> {
                evt.ctrlKey // Windows, Linux
                        || evt.metaKey // MacOS
                        || (evt.button == 1.toShort()) // Middle Mouse button
            }
        }
    }

    /**
     * Navigates to the given uri.
     */
    fun navToUri(uri: String) {
        if (window.location.hash != uri) {
            window.location.hash = uri
        } else {
            navigateToWindowUri()
        }
    }

    /**
     * Navigate to the given [uri].
     *
     * If [evt].ctrlKey is true, the uri will be opened in a new tab.
     */
    fun navToUri(evt: MouseEvent?, uri: String) {
        return navToUri(
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
        if (target == null) {
            navToUri(uri)
        } else {
            window.open(uri, target = target)
        }
    }

    /**
     * Navigates to the [route] by filling in the given [routeParams] and [queryParams].
     */
    fun navToUri(route: Route, routeParams: Map<String, String>, queryParams: Map<String, String>) {
        (route as? Route.Renderable)?.let {
            navToUri(
                it.buildUri(routeParams = routeParams, queryParams = queryParams)
            )
        }
    }

    /**
     * Navigates to the [route].
     */
    fun navToUri(route: Route.Match) {
        navToUri(route = route.route, routeParams = route.routeParams, queryParams = route.queryParams)
    }

    /**
     * Replaces the current uri with changing the history
     */
    fun replaceUri(uri: String) {

        if (window.location.hash != uri) {
            window.history.replaceState(
                null,
                "",
                window.location.pathname + uri
            )
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
        (route as? Route.Renderable)?.let {
            replaceUri(
                it.buildUri(routeParams = routeParams, queryParams = queryParams)
            )
        }
    }

    /**
     * Replaces the current uri without changing the browser history.
     */
    fun replaceUri(route: Route.Match) {
        replaceUri(
            route.route, routeParams = route.routeParams, queryParams = route.queryParams
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
        val uri = window.location.hash.removePrefix(prefix)

        val resolved = resolveRouteForUri(uri)

        if (resolved == null) {
            console.error("Could not resolve route: $uri")
        }

        resolved?.let { (mounted, match) ->
            // Create a context for the router middlewares
            val ctx = RouterMiddlewareContext(this, uri, match)
            // Call all the middlewares
            mounted.middlewares.forEach { it(ctx) }

            // Do we have a redirect ?
            when (val redirect = ctx.redirectToUri) {
                null -> {
                    // Notify all subscribers that the active route has changed
                    _current(ActiveRoute(uri, match, mounted))
                }

                else -> {
                    // Yes so let's go to the redirect
                    navToUri(redirect)
                }
            }
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

    /**
     * Private listener for the "hashchange" event
     */
    private fun windowListener(event: Event) {
        if (enabled) {
            event.preventDefault()

            navigateToWindowUri()
        }
    }
}
