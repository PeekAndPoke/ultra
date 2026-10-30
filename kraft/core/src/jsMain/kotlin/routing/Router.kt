package io.peekandpoke.kraft.routing

import io.peekandpoke.kraft.KraftApp
import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.getAttributeRecursive
import io.peekandpoke.kraft.routing.Router.RouterStrategy.Companion.HASH_PREFIX
import io.peekandpoke.kraft.utils.launch
import io.peekandpoke.ultra.common.TypedKey
import io.peekandpoke.ultra.streams.Stream
import io.peekandpoke.ultra.streams.StreamSource
import io.peekandpoke.ultra.streams.Unsubscribe
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.Job
import org.w3c.dom.Document
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.Node
import org.w3c.dom.events.Event
import org.w3c.dom.events.EventTarget
import org.w3c.dom.events.MouseEvent
import org.w3c.dom.url.URL
import kotlin.js.Date

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

        /** How long after [withoutLeaveGuards] a beforeunload still counts as started inside it. */
        private const val UNLOAD_APPROVAL_MS = 3000

        private var restoredCounter = 0

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

        /**
         * Writes [uri] into the browser's address in place of the current entry, without adding one. Only writes:
         * [Router.replaceUri] resolves the route afterwards.
         */
        fun replaceUri(uri: String)

        /** Returns the current URI from the browser's location. */
        fun getUriFromWindowLocation(): String

        /** Removes the browser event listeners [init] installed. */
        fun dispose() {}

        /**
         * The [uri] as [getUriFromWindowLocation] would read it back once it is the address, in the browser's encoding.
         * The default returns it unchanged: a strategy whose [render] output differs from what it reads back (a `#!`
         * prefix) overrides it, or every navigation counts as leaving the page.
         */
        fun normalize(uri: String): String = uri

        /**
         * Puts [uri] back as the address, as a new entry and without telling the router: a [LeaveGuard] that stops a
         * Back, a Forward or a typed hash undoes the browser's move with it. Returns whether it did; the default does
         * nothing and returns false, and the router then leaves the address where the browser moved it.
         */
        fun restoreUri(uri: String): Boolean = false
    }

    /** Hash-based routing strategy using the URL fragment (e.g. `#/path`). */
    class HashRoutingStrategy(private val router: Router) : RouterStrategy {

        // One reference per listener: each `::x` is a new function object, and removal needs the one that was added
        private val onHashChange: (Event) -> Unit = ::windowListener
        private val onClick: (Event) -> Unit = ::clickListener

        override fun init() {
            window.addEventListener("hashchange", onHashChange)
            // Route links ("#/...") go through the router, so a leave guard holds them before the browser writes
            window.addEventListener("click", onClick)
        }

        override fun dispose() {
            window.removeEventListener("hashchange", onHashChange)
            window.removeEventListener("click", onClick)
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
            // Into the fragment, where this strategy keeps its routes; a bare path would drop the fragment. The entry's
            // state stays: a BackNavigationTrap marks its entry with it
            window.history.replaceState(data = window.history.state, title = "", url = hashUrl(uri))
        }

        override fun getUriFromWindowLocation(): String {
            return window.location.hash.cleanUri()
        }

        override fun normalize(uri: String): String = try {
            // The fragment as location.hash will hold it: percent-encoded by the browser's URL parser
            URL(HASH_PREFIX + uri.cleanUri(), window.location.href).hash.cleanUri()
        } catch (_: Throwable) {
            uri.cleanUri()
        }

        override fun restoreUri(uri: String): Boolean {
            window.history.pushState(data = null, title = "", url = hashUrl(uri))
            return true
        }

        /** The document's own URL with [uri] as its fragment: a bare "#/x" would resolve against a `<base href>`. */
        private fun hashUrl(uri: String): String =
            window.location.href.substringBefore(HASH_PREFIX) + HASH_PREFIX + uri.cleanUri()

        private fun String.cleanUri(): String {
            return removePrefix(HASH_PREFIX).trim()
        }

        /**
         * Private listener for the "hashchange" event
         */
        private fun windowListener(event: Event) {
            if (router.enabled) {
                event.preventDefault()

                router.followBrowserNavigation()
            }
        }

        /** Plain clicks on route links (`href="#/..."`) go through the router; in-page anchors stay the browser's. */
        @Suppress("detekt:ReturnCount")
        private fun clickListener(event: Event) {
            if (!router.enabled) return

            val mouseEvent = event as? MouseEvent ?: return

            if (!mouseEvent.isPlainPrimaryClick()) return

            val anchor = findClosestAnchor(mouseEvent.target) ?: return
            val href = anchor.getAttribute("href") ?: return

            if (!href.startsWith(HASH_PREFIX + "/")) return
            if (!anchor.targetsSelf() || anchor.hasAttribute("download")) return
            // Already the address (an onClick of the link navigated first): the browser does nothing either
            if (normalize(href) == getUriFromWindowLocation()) return

            event.preventDefault()
            router.navToUri(href)
        }
    }

    /** Path-based routing strategy using the browser's History API. */
    class PathRoutingStrategy(private val router: Router) : RouterStrategy {
        private companion object {
            /** RFC 3986 scheme followed by a colon, at the start of an href. */
            val AbsoluteUrlSchemeRegex = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*:")
        }

        // One reference per listener: each `::x` is a new function object, and removal needs the one that was added
        private val onPopState: (Event) -> Unit = ::popstateListener
        private val onClick: (Event) -> Unit = ::clickListener

        override fun init() {
            window.addEventListener("popstate", onPopState)
            // Intercept clicks on links to prevent page reloads
            window.addEventListener("click", onClick)
        }

        override fun dispose() {
            window.removeEventListener("popstate", onPopState)
            window.removeEventListener("click", onClick)
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
            // The entry's state stays: a BackNavigationTrap marks its entry with it
            window.history.replaceState(window.history.state, "", uri.cleanUri())
        }

        override fun getUriFromWindowLocation(): String {
            return window.location.pathname + window.location.search + window.location.hash
        }

        override fun normalize(uri: String): String = try {
            // As location will hold it: percent-encoded by the browser's URL parser
            URL(uri.cleanUri(), window.location.href).let { it.pathname + it.search + it.hash }
        } catch (_: Throwable) {
            uri.cleanUri()
        }

        override fun restoreUri(uri: String): Boolean {
            window.history.pushState(null, "", uri.cleanUri())
            return true
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

            router.followBrowserNavigation()
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

            // Someone else already handled it, or not a plain primary click: new tab, new window, download, ...
            if (!mouseEvent.isPlainPrimaryClick()) return

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

    /** The registered [LeaveGuard]s, in mount order. */
    private val leaveGuards = mutableListOf<RegisteredGuard>()

    /**
     * A guard with the page (its path) it was registered on: it protects leaving that page only. `null`: registered
     * before the router had a route (an app shell), on no page, so never asked about a navigation.
     */
    private class RegisteredGuard(val guard: LeaveGuard, val page: String?)

    /** An [LeaveResult.Ask] is waiting for its answer: further navigations away are dropped meanwhile. */
    private var askingToLeave = false

    /** A navigation the guards already let through: its own address change (a hashchange) is not asked again. */
    private var letThrough: String? = null

    /** A navigation the guards let through is running: what it leads to (a middleware redirect) follows unasked. */
    private var approved = false

    /** [withoutLeaveGuards] runs: unlike the router's own approvals, it also approves a page load. */
    private var unloadApproved = false

    /** Until when a page load counts as started inside [withoutLeaveGuards]: browsers may fire beforeunload later. */
    private var unloadApprovedUntil = 0.0

    /** True while [withoutLeaveGuards] runs, and shortly after: `guardLeaving`'s beforeunload prompt stays quiet. */
    internal val passingLeaveGuards: Boolean get() = unloadApproved || Date.now() < unloadApprovedUntil


    /** The pending [LeaveResult.Ask], cancelled when the router is disposed or leaves its page past the guards. */
    private var asking: Job? = null

    /** The page the pending [asking] protects. */
    private var askingFromPage: String? = null

    private var disposed = false

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

    /** Disables the router for good and removes its browser event listeners; for an app that is torn down. */
    fun dispose() {
        disposed = true
        disable()
        strategy.dispose()
        // An unanswered Ask must not navigate a torn-down app later; cancelling also closes its modal
        asking?.cancel()
    }

    /**
     * Navigates to the given uri.
     */
    fun navToUri(uri: String) {
        guarded(uri) { strategy.navigateToUri(uri) }
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
        guarded(uri) {
            val before = strategy.getUriFromWindowLocation()

            strategy.replaceUri(uri)

            // Compared as the strategy reads the address back, so "#/x" and "/x" are the same under hash routing
            if (strategy.getUriFromWindowLocation() != before) {
                // The replaced route leaves the history
                _historyEntries.removeLastOrNull()
                // Resolve the next route, once
                navigateToWindowUri()
            }
        }
    }

    /**
     * Registers a [LeaveGuard] for the page the router is on, asked before each navigation away from it until the
     * returned [Unsubscribe] is called; [guardLeaving] registers for as long as its component is mounted.
     */
    internal fun addLeaveGuard(guard: LeaveGuard): Unsubscribe {
        val onNoPage = current().mountedRoute == MountedRoute.default

        if (onNoPage) {
            console.warn(
                "guardLeaving registered before the router is on a route (outside the routed page?): " +
                    "it is never asked about navigations, only its beforeunload prompt works"
            )
        }

        val registered = RegisteredGuard(guard, if (onNoPage) null else pagePath(current().uri))

        leaveGuards += registered

        return { leaveGuards.remove(registered) }
    }

    /**
     * Runs [block] past every [LeaveGuard]: the router navigations it starts (`navToUri`, `replaceUri`, `navBack`),
     * and what they lead to, are not asked, and a pending Ask is dropped. A page load it starts (a form submit,
     * `location.href`) gets no beforeunload prompt. For a leave that must happen whatever a page holds, e.g. a logout:
     * `router.withoutLeaveGuards { router.navToUri("/login") }`. A `history.back()` inside is the browser's and still asked:
     * use `navBack()`.
     */
    fun <R> withoutLeaveGuards(block: () -> R): R {
        val was = unloadApproved
        unloadApproved = true

        try {
            return approving(block)
        } finally {
            unloadApproved = was
            // A page load the block started may fire beforeunload a little later
            unloadApprovedUntil = Date.now() + UNLOAD_APPROVAL_MS
        }
    }

    /** The router's own approvals: past the guards, but no page load is approved by them. */
    private fun <R> approving(block: () -> R): R {
        val was = approved
        approved = true

        try {
            return block()
        } finally {
            approved = was
        }
    }

    /** Runs [go] as a navigation let through to [target]; a [go] that throws lets nothing through later. */
    private fun letThroughTo(target: String, go: () -> Unit) {
        letThrough = target
        var gone = false

        try {
            go()
            gone = true
        } finally {
            if (!gone) letThrough = null
        }
    }

    /** Runs [go] if no guard holds the navigation to [uri]; with an [LeaveResult.Ask], once it is answered yes. */
    private fun guarded(uri: String, go: () -> Unit) {
        val target = strategy.normalize(uri)

        // Within a navigation already let through (a middleware redirect), or past the guards on purpose
        if (approved) {
            // A pending Ask about the page left this way is moot
            cancelAskIfLeaving(target)
            return letThroughTo(target, go)
        }

        if (pageGuards().isEmpty() || !isLeaving(target)) return go()
        if (askingToLeave) return

        val pass = {
            approving { letThroughTo(target, go) }
        }

        when (val answer = askGuards(target)) {
            LeaveResult.Proceed -> pass()
            LeaveResult.Stop -> Unit
            is LeaveResult.Ask -> {
                // A yes still goes, after a query-only move meanwhile (a urlParam): the same page asked
                val fromPage = pagePath(current().uri)
                ask(answer, stillThere = { pagePath(current().uri) == fromPage }, go = pass)
            }
        }
    }

    /**
     * Follows an address the browser moved itself (Back, Forward, a typed hash), unless a [LeaveGuard] holds it: then
     * the page's address is put back with [RouterStrategy.restoreUri], and an [LeaveResult.Ask] answered yes replays
     * the move by going back from that restored entry. The built-in strategies call it from their `popstate` /
     * `hashchange` listeners; a custom strategy does the same to take part in the guards.
     */
    fun followBrowserNavigation() {
        val target = strategy.getUriFromWindowLocation()

        letThrough?.let { through ->
            if (sameUri(target, through)) {
                letThrough = null
                // Arriving past the guards: a pending Ask about the page left this way is moot
                cancelAskIfLeaving(target)
                approving { navigateToWindowUri() }
                return
            }
        }

        if (approved || pageGuards().isEmpty() || !isLeaving(target)) return navigateToWindowUri()

        val stay = current().uri

        if (askingToLeave) {
            strategy.restoreUri(stay)
            return
        }

        when (val answer = askGuards(target)) {
            LeaveResult.Proceed -> approving { navigateToWindowUri() }

            LeaveResult.Stop -> strategy.restoreUri(stay)

            is LeaveResult.Ask -> {
                val restored = strategy.restoreUri(stay)
                val marker = if (restored) markRestoredEntry() else null
                val fromPage = pagePath(current().uri)

                // A yes still goes after a query-only move meanwhile; the marker says where back would start
                ask(answer, stillThere = { pagePath(current().uri) == fromPage }) {
                    when {
                        // On the restored entry, pushed on top of the target's: back is the move itself
                        marker != null && window.history.state == marker -> {
                            letThrough = target
                            window.history.back()
                        }

                        // Restored, but something stands on top of it (a confirm UI's own entry): go there instead,
                        // let through so its own hashchange is not asked again
                        marker != null -> approving { navToUri(target) }

                        // Nothing was put back: the browser is still at the target
                        else -> approving { navigateToWindowUri() }
                    }
                }
            }
        }
    }

    /** The same address, also when the browser percent-encoded one of them (`location.hash` does). */
    private fun sameUri(a: String, b: String): Boolean = a == b || decoded(a) == decoded(b)

    private fun decoded(uri: String): String = try {
        js("decodeURI")(uri).unsafeCast<String>()
    } catch (_: Throwable) {
        uri
    }

    /**
     * Leaving: the [target] (normalized) is on another path. The path is what RouterComponent keys the page on, so a
     * guard is asked exactly when the page would be remounted; a change of query or fragment alone never asks.
     */
    private fun isLeaving(target: String): Boolean = pagePath(target) != pagePath(current().uri)

    /** The path of [uri], before `?` and `#`: the identity of a page. */
    private fun pagePath(uri: String): String = uri.substringBefore('#').substringBefore('?')

    /** Marks the current history entry (a restored one) and returns the marker. */
    private fun markRestoredEntry(): String {
        val marker = "--kraft-leave-restored-${restoredCounter++}--"
        window.history.replaceState(marker, "", window.location.href)
        return marker
    }

    /**
     * The guards of the current page, in mount order. A guard still registered for the page just left (its component
     * unmounts a render after the route changed) is not asked about the next navigation.
     */
    private fun pageGuards(): List<LeaveGuard> {
        val page = pagePath(current().uri)

        return leaveGuards.filter { it.page != null && it.page == page }.map { it.guard }
    }

    /** A pending Ask protects its page: a navigation past the guards to another path drops it (and closes its modal). */
    private fun cancelAskIfLeaving(target: String) {
        val pending = asking ?: return

        if (pagePath(target) != askingFromPage) {
            pending.cancel()
            // At once: a job cancelled before it ran never reaches its own finally
            stopAsking(pending)
        }
    }

    /** The first answer that does not proceed, in mount order. */
    private fun askGuards(target: String): LeaveResult =
        pageGuards().asSequence()
            .map { it.onLeave(target) }
            .firstOrNull { it != LeaveResult.Proceed }
            ?: LeaveResult.Proceed

    /**
     * Asks [answer], and runs [go] on a yes, but only if the router is [stillThere]: a navigation past the guards
     * meanwhile (a logout) or a disposed router drops the answer.
     */
    private fun ask(answer: LeaveResult.Ask, stillThere: () -> Boolean, go: () -> Unit) {
        askingToLeave = true
        askingFromPage = pagePath(current().uri)

        lateinit var job: Job

        job = launch {
            val yes = try {
                answer.confirm()
            } finally {
                stopAsking(job)
            }

            if (yes && !disposed && stillThere()) go()
        }

        asking = job
        // Also for a job cancelled before it ran: its body, and its finally, never run
        job.invokeOnCompletion { stopAsking(job) }
    }

    /** The Ask [job] is over; a newer Ask keeps its own state. */
    private fun stopAsking(job: Job) {
        if (asking !== job) return

        askingToLeave = false
        asking = null
        askingFromPage = null
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
        // Cleared only once no guard holds the navigation
        guarded(uri) {
            _historyEntries.clear()
            strategy.navigateToUri(uri)
        }
    }

    /**
     * Navigates to the previous route, when this is possible.
     */
    fun navBack() {
        if (history().canGoBack) {
            val previous = _historyEntries[_historyEntries.lastIndex - 1]

            // The entries go only once no guard holds the navigation
            guarded(previous.uri) {
                // remove the current active route
                _historyEntries.removeLast()
                // remove the previous active route and go back to it
                _historyEntries.removeLast()
                strategy.navigateToUri(previous.uri)
            }
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
        // A let-through navigation has arrived (or never comes as its own event, under the path strategy)
        letThrough = null

        // get the location from the browser
        val uri = strategy.getUriFromWindowLocation()

        // On another page by any way (also one whose guard unmounted while it asked): its pending Ask is moot
        cancelAskIfLeaving(uri)

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

/** Not handled yet, the primary button, no modifier key: a click the router may take over. */
private fun MouseEvent.isPlainPrimaryClick(): Boolean =
    !defaultPrevented && button == 0.toShort() && !(ctrlKey || metaKey || shiftKey || altKey)
