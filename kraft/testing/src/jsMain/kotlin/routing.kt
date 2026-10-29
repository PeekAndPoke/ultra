package io.peekandpoke.kraft.testing

import io.peekandpoke.kraft.routing.Router
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/** Waits until the router's current route has [uri]; fails with where the router is after [timeout]. */
suspend fun Router.awaitUri(uri: String, timeout: Duration = 2.seconds) {
    eventually(timeout = timeout) {
        val at = current().uri

        if (at != uri) throw AssertionError("router at '$at', expected '$uri'")
    }
}
