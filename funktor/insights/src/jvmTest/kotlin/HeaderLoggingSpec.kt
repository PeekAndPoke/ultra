package io.peekandpoke.funktor.insights

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.maps.shouldNotContainKey
import io.kotest.matchers.shouldBe

class HeaderLoggingSpec : StringSpec({

    val redacted = listOf(HeaderLogging.REDACTED)

    "an exact rule matches case-insensitively in both directions" {
        val policy = HeaderLogging.defaults.with("X-Custom-Thing", HeaderAction.DROP)

        policy.actionFor("x-custom-thing") shouldBe HeaderAction.DROP
        policy.actionFor("X-CUSTOM-THING") shouldBe HeaderAction.DROP
        // and the built-in rules, which are declared lower-case, match an upper-case header
        policy.actionFor("AUTHORIZATION") shouldBe HeaderAction.REDACT
    }

    "a regex rule is matched against the lower-cased name" {
        val policy = HeaderLogging.defaults.with(Regex("^x-debug-.*"), HeaderAction.DROP)

        policy.actionFor("X-Debug-Trace") shouldBe HeaderAction.DROP
        policy.actionFor("x-debug-") shouldBe HeaderAction.DROP
        // `matches` is whole-string: a name merely CONTAINING the pattern must not match
        policy.actionFor("prefix-x-debug-trace") shouldBe HeaderAction.LOG
    }

    "the last matching rule wins, so an app overrides a default" {
        // `authorization` is REDACT by default; a later rule must be able to beat it
        HeaderLogging.defaults
            .with("authorization", HeaderAction.DROP)
            .actionFor("authorization") shouldBe HeaderAction.DROP

        // and order among the app's own rules is last-wins too
        HeaderLogging.defaults
            .with(Regex("^x-.*"), HeaderAction.DROP)
            .with("x-keep-me", HeaderAction.LOG)
            .actionFor("x-keep-me") shouldBe HeaderAction.LOG
    }

    "an exact rule does NOT implicitly beat a regex added after it" {
        // Precedence is purely positional. Pinning it so nobody "fixes" it into kind-based precedence
        // without deciding to.
        HeaderLogging.defaults
            .with("x-thing", HeaderAction.LOG)
            .with(Regex("^x-.*"), HeaderAction.DROP)
            .actionFor("x-thing") shouldBe HeaderAction.DROP
    }

    "the sensitive-name regex catches headers nobody listed" {
        val policy = HeaderLogging.defaults

        // none of these are in the known-sensitive list
        policy.actionFor("x-amz-security-token") shouldBe HeaderAction.REDACT
        policy.actionFor("x-my-app-secret") shouldBe HeaderAction.REDACT
        policy.actionFor("x-session-id") shouldBe HeaderAction.REDACT
        policy.actionFor("x-refresh-token") shouldBe HeaderAction.REDACT
        policy.actionFor("x-apikey") shouldBe HeaderAction.REDACT
        policy.actionFor("x-api-key-hint") shouldBe HeaderAction.REDACT
    }

    "the known-sensitive list is not shadowed by the heuristic" {
        // `.*auth.*` also matches `authorization`, so if the pattern rule were added last it would win
        // on precedence. Both say REDACT today, which makes the ordering invisible — this pins it, so
        // changing the pattern's action cannot silently un-redact a credential header.
        HeaderLogging.defaults
            .actionFor("authorization") shouldBe HeaderAction.REDACT

        HeaderLogging.strict.withDefault(HeaderAction.LOG)
            .actionFor("proxy-authorization") shouldBe HeaderAction.REDACT
    }

    "an explicit LOG beats the sensitive-name heuristic" {
        // the escape hatch for a header that merely looks sensitive
        HeaderLogging.defaults
            .with("x-trace-token", HeaderAction.LOG)
            .actionFor("x-trace-token") shouldBe HeaderAction.LOG
    }

    "ordinary headers keep their values by default" {
        val policy = HeaderLogging.defaults

        policy.actionFor("user-agent") shouldBe HeaderAction.LOG
        policy.actionFor("content-type") shouldBe HeaderAction.LOG
        policy.actionFor("accept-language") shouldBe HeaderAction.LOG
    }

    "REDACT keeps the name and replaces every value" {
        val result = HeaderLogging.defaults.applyTo(
            mapOf("Authorization" to listOf("Bearer abcdefghijklmnop", "Bearer second"))
        )

        result["Authorization"] shouldBe listOf(HeaderLogging.REDACTED, HeaderLogging.REDACTED)
    }

    "DROP removes the header entirely, name included" {
        val result = HeaderLogging.defaults
            .with("x-noise", HeaderAction.DROP)
            .applyTo(mapOf("x-noise" to listOf("v"), "user-agent" to listOf("curl")))

        result shouldNotContainKey "x-noise"
        result["user-agent"] shouldBe listOf("curl")
    }

    "a short Authorization value is redacted, not truncated — and does not throw" {
        // The previous implementation did `substring(0, 20)`, which threw here and, for longer Basic
        // credentials, kept ten decoded bytes of `user:password`.
        val result = HeaderLogging.defaults.applyTo(mapOf("authorization" to listOf("Bearer abc")))

        result["authorization"] shouldBe redacted
    }

    "no part of a redacted value survives" {
        val secret = "Basic dXNlcjpzdXBlcnNlY3JldHBhc3N3b3Jk"
        val result = HeaderLogging.defaults.applyTo(mapOf("authorization" to listOf(secret)))

        val rendered = result.toString()
        rendered.contains("dXNlcjpz") shouldBe false
        rendered.contains("Basic") shouldBe false
    }

    "Cookie and Set-Cookie are redacted — a login record must not carry a session" {
        val result = HeaderLogging.defaults.applyTo(
            mapOf(
                "Cookie" to listOf("session=abc123"),
                "Set-Cookie" to listOf("session=abc123; HttpOnly"),
            )
        )

        result["Cookie"] shouldBe redacted
        result["Set-Cookie"] shouldBe redacted
    }

    "cookie variants cannot fail open" {
        // `cookie` is on the exact list, but that list can never cover every variant. The alternation
        // is what catches these — without it, cookie2/set-cookie2 (RFC 2965) default to LOG.
        HeaderLogging.defaults.actionFor("cookie2") shouldBe HeaderAction.REDACT
        HeaderLogging.defaults.actionFor("set-cookie2") shouldBe HeaderAction.REDACT
        HeaderLogging.defaults.actionFor("x-my-cookie-jar") shouldBe HeaderAction.REDACT
    }

    "request signatures are redacted" {
        HeaderLogging.defaults.actionFor("stripe-signature") shouldBe HeaderAction.REDACT
        HeaderLogging.defaults.actionFor("x-hub-signature-256") shouldBe HeaderAction.REDACT
        HeaderLogging.defaults.actionFor("x-signature") shouldBe HeaderAction.REDACT
    }

    "Location loses its query string — a redirect is where a grant is MINTED" {
        // Referer only echoes a token back; Location is where the authorization server hands one over.
        // Neither name matches the sensitive-name alternation, so both used to be stored verbatim.
        val result = HeaderLogging.defaults.applyTo(
            mapOf(
                "Location" to listOf("https://app.example.com/cb?code=SECRET-GRANT&state=xyz"),
                "Content-Location" to listOf("/download?sig=SECRET-SIG"),
            )
        )

        result["Location"] shouldBe listOf("https://app.example.com/cb")
        result["Content-Location"] shouldBe listOf("/download")
        result.toString().contains("SECRET-GRANT") shouldBe false
        result.toString().contains("SECRET-SIG") shouldBe false
    }

    "api_key is matched, not only api-key and apikey" {
        // The pattern is reused for QUERY PARAMETER names, which are snake_case by convention while
        // header names are kebab-case. `api-?key` matched neither `api_key` nor the example in its own
        // KDoc; `?api_key=` was therefore stored verbatim.
        HeaderLogging.defaults.actionFor("api_key") shouldBe HeaderAction.REDACT
        HeaderLogging.defaults.actionFor("api-key") shouldBe HeaderAction.REDACT
        HeaderLogging.defaults.actionFor("apikey") shouldBe HeaderAction.REDACT

        HeaderLogging.defaults.applyToQueryParams(
            mapOf("api_key" to listOf("SECRET-VALUE"))
        )["api_key"] shouldBe redacted
    }

    "referer keeps its path but loses its query string" {
        // The referer is the page the user came FROM — which is exactly where a magic-link or
        // password-reset token sits. Dropping the header loses real debugging value; keeping it whole
        // leaks the token.
        val result = HeaderLogging.defaults.applyTo(
            mapOf("Referer" to listOf("https://app.example.com/reset?token=SECRET-VALUE"))
        )

        result["Referer"] shouldBe listOf("https://app.example.com/reset")
        result.toString().contains("SECRET-VALUE") shouldBe false
    }

    "a referer with no query is untouched" {
        HeaderLogging.defaults.applyTo(mapOf("referer" to listOf("https://app.example.com/page")))
            .get("referer") shouldBe listOf("https://app.example.com/page")
    }

    "query parameters are redacted by the same rules as headers" {
        // Headers were never the only place a credential travels. Before this, `?token=` was stored
        // verbatim AND reflected into the summary url.
        val result = HeaderLogging.defaults.applyToQueryParams(
            mapOf(
                "token" to listOf("SECRET-VALUE"),
                "code" to listOf("oauth-code"),
                "access_token" to listOf("SECRET-VALUE"),
                "page" to listOf("2"),
            )
        )

        result["token"] shouldBe redacted
        result["access_token"] shouldBe redacted
        result["page"] shouldBe listOf("2")
        result.toString().contains("SECRET-VALUE") shouldBe false
    }

    "an app can extend the policy to its own query parameter" {
        // `code` is not sensitive by name, but an OAuth callback's is.
        val policy = HeaderLogging.defaults.with("code", HeaderAction.REDACT)

        policy.applyToQueryParams(mapOf("code" to listOf("oauth-code")))["code"] shouldBe redacted
    }

    "strict redacts everything unmatched, defaults do not" {
        HeaderLogging.strict.actionFor("user-agent") shouldBe HeaderAction.REDACT
        HeaderLogging.defaults.actionFor("user-agent") shouldBe HeaderAction.LOG

        // strict still honours an explicit LOG
        HeaderLogging.strict.with("user-agent", HeaderAction.LOG)
            .actionFor("user-agent") shouldBe HeaderAction.LOG
    }

    "withDefault changes only the fallback, leaving rules intact" {
        val policy = HeaderLogging.defaults.withDefault(HeaderAction.DROP)

        policy.actionFor("user-agent") shouldBe HeaderAction.DROP
        policy.actionFor("authorization") shouldBe HeaderAction.REDACT
    }

    "a policy is immutable — `with` returns a new instance" {
        val base = HeaderLogging.defaults
        base.with("user-agent", HeaderAction.DROP)

        base.actionFor("user-agent") shouldBe HeaderAction.LOG
    }

    "applyTo preserves headers with no values and multi-valued ones" {
        val result = HeaderLogging.defaults.applyTo(
            mapOf("x-empty" to emptyList(), "accept" to listOf("a", "b"))
        )

        result["x-empty"] shouldBe emptyList()
        result["accept"] shouldBe listOf("a", "b")
    }
})
