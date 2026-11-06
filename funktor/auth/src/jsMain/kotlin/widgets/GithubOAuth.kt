package io.peekandpoke.funktor.auth.widgets

import kotlinx.browser.window
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.khronos.webgl.Uint8Array
import org.khronos.webgl.get
import org.w3c.dom.Storage
import org.w3c.dom.url.URLSearchParams

/**
 * GitHub's OAuth round trip, done safely (ULTRA-00288).
 *
 * - **Login CSRF:** [begin] stores a random `state` in sessionStorage and sends it along; [complete] accepts a
 *   callback only with that state, once. A link carrying an attacker's `code` signs nobody into the attacker's account.
 * - **The route rides in the state, not in the redirect URI.** A redirect URI may not carry a fragment
 *   (RFC 6749 §3.1.2), and under hash routing everything after `#` swallowed `&scope=user:email`, which every GitHub
 *   sign-in needs. The action (sign in or up) and the hash to return to are stored with the state.
 */
object GithubOAuth {
    /** What the round trip was started for. */
    enum class Action { SignIn, SignUp }

    /** One started round trip, as stored until its callback. */
    @Serializable
    internal data class Pending(val state: String, val action: Action, val hash: String)

    internal const val STORAGE_KEY = "funktor-auth-github-oauth"

    private const val AUTHORIZE_URL = "https://github.com/login/oauth/authorize"

    /** Per tab, gone with it: a round trip belongs to the tab that started it. */
    private val storage: Storage get() = window.sessionStorage

    /** Starts a round trip for [action], to return to [hash]; the authorize URL to send the browser to. */
    internal fun begin(clientId: String, redirectUri: String, action: Action, hash: String): String {
        val state = randomState()

        storage.setItem(STORAGE_KEY, Json.encodeToString(Pending.serializer(), Pending(state, action, hash)))

        return authorizeUrl(clientId = clientId, redirectUri = redirectUri, state = state)
    }

    /** The authorize URL: every value encoded, the scope first. */
    internal fun authorizeUrl(clientId: String, redirectUri: String, state: String): String =
        AUTHORIZE_URL +
                "?scope=" + encode("user:email") +
                "&client_id=" + encode(clientId) +
                "&redirect_uri=" + encode(redirectUri) +
                "&state=" + encode(state)

    /** The round trip a callback with [state] completes, once; null for none, another one, or a missing state. */
    internal fun complete(state: String?): Pending? {
        val pending = peek()
        // Single use, whatever the outcome.
        storage.removeItem(STORAGE_KEY)

        return pending?.takeIf { state != null && it.state == state }
    }

    /**
     * Under hash routing GitHub's redirect lands on the app's ROOT route: the redirect URI may carry no fragment. When
     * the URL is a callback ([callbackParam] in the query) for a round trip this tab started, puts back the route it
     * started on, before the router reads the URL, so the login page that handles the callback is the one shown.
     * Consumes nothing; [complete] does. Called when the auth state is created.
     */
    internal fun returnToStartedRoute(callbackParam: String) {
        if (!URLSearchParams(window.location.search).has(callbackParam)) return

        val hash = peek()?.hash?.takeIf { it.isNotEmpty() } ?: return

        if (window.location.hash != hash) {
            window.history.replaceState(null, "", window.location.pathname + window.location.search + hash)
        }
    }

    private fun peek(): Pending? = storage.getItem(STORAGE_KEY)
        ?.let { runCatching { Json.decodeFromString(Pending.serializer(), it) }.getOrNull() }

    /** 128 random bits from the browser's CSPRNG, as hex. */
    private fun randomState(): String {
        val bytes: Uint8Array = window.asDynamic().crypto.getRandomValues(Uint8Array(16)).unsafeCast<Uint8Array>()

        return (0 until bytes.length).joinToString("") { i -> bytes[i].toInt().and(0xFF).toString(16).padStart(2, '0') }
    }

    private fun encode(value: String): String = js("encodeURIComponent")(value).unsafeCast<String>()
}
