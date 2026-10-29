package io.peekandpoke.kraft.testing

import io.peekandpoke.kraft.routing.Router
import kotlinx.browser.window
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Waits until the router's current route has [uri], and then for one animation frame, so the routed page has rendered
 * (under path routing the route changes at once, its page a render later); fails with where the router is after
 * [timeout].
 */
suspend fun Router.awaitUri(uri: String, timeout: Duration = 2.seconds) {
    eventually(timeout = timeout) {
        val at = current().uri

        if (at != uri) throw AssertionError("router at '$at', expected '$uri'")
    }

    suspendCancellableCoroutine { cont -> window.requestAnimationFrame { cont.resume(Unit) } }
}
