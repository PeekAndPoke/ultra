package io.peekandpoke.funktor.auth.widgets

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldMatch
import io.kotest.matchers.string.shouldNotContain
import io.kotest.matchers.string.shouldStartWith
import kotlinx.browser.window
import org.w3c.dom.url.URL

/**
 * GitHub's OAuth round trip (ULTRA-00288): the state that stops login CSRF, and an authorize URL whose parameters
 * all reach GitHub, also under hash routing.
 */
class GithubOAuthSpec : StringSpec({

    val redirectUri = "https://app.example.com/login?auth-callback=github"

    beforeTest { window.sessionStorage.removeItem(GithubOAuth.STORAGE_KEY) }

    fun stateOf(url: String): String = URL(url).searchParams.get("state").shouldNotBeNull()

    "the authorize URL carries every value encoded, the scope first, and no fragment" {
        val url = GithubOAuth.authorizeUrl(clientId = "client&1", redirectUri = "$redirectUri#/route", state = "s")

        url shouldStartWith "https://github.com/login/oauth/authorize?scope=user%3Aemail&"
        url shouldNotContain "#"

        val params = URL(url).searchParams
        params.get("scope") shouldBe "user:email"
        params.get("client_id") shouldBe "client&1"
        params.get("redirect_uri") shouldBe "$redirectUri#/route"
        params.get("state") shouldBe "s"
    }

    "begin stores a fresh random state; the callback with it completes the round trip ONCE, with its action and route" {
        val url = GithubOAuth.begin("client", redirectUri, GithubOAuth.Action.SignUp, hash = "#/signup")
        val state = stateOf(url)

        state shouldMatch Regex("[0-9a-f]{32}")
        stateOf(GithubOAuth.begin("client", redirectUri, GithubOAuth.Action.SignUp, hash = "#/signup")) shouldNotBe state

        // The second begin replaced the first: only its state completes.
        val current = stateOf(GithubOAuth.begin("client", redirectUri, GithubOAuth.Action.SignIn, hash = "#/login"))
        val pending = GithubOAuth.complete(current).shouldNotBeNull()
        pending.action shouldBe GithubOAuth.Action.SignIn
        pending.hash shouldBe "#/login"

        GithubOAuth.complete(current).shouldBeNull()
    }

    "a callback with another state, or none, completes nothing, and burns the pending one" {
        listOf("forged", null).forEach { wrong ->
            val state = stateOf(GithubOAuth.begin("client", redirectUri, GithubOAuth.Action.SignIn, hash = ""))

            GithubOAuth.complete(wrong).shouldBeNull()
            GithubOAuth.complete(state).shouldBeNull()
        }
    }

    "a callback that no begin in this browser started completes nothing" {
        GithubOAuth.complete("anything").shouldBeNull()
    }
})
