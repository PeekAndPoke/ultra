package io.peekandpoke.funktor.insights

/** What happens to one header when an insights record is written. */
enum class HeaderAction {
    /** Record the name and the value. */
    LOG,

    /** Record the name, replace the value — proves presence without disclosing content. */
    REDACT,

    /** Omit the header entirely; not even its name is recorded. */
    DROP,

    /**
     * Keep the value up to the `?`, discard the query string.
     *
     * For URL-bearing headers — `Referer`, which carries the page the user came FROM, and `Location`,
     * which is where a redirect-borne grant is minted. Dropping them outright would lose real debugging
     * value; keeping them whole leaks the token.
     *
     * **Only the query string.** A secret in the PATH (`/reset/<jwt>` — the commonest magic-link shape),
     * in the fragment, or in userinfo (`https://user:pass@host/`) survives untouched. Use
     * [HeaderAction.REDACT] for a header whose URLs carry secrets outside the query.
     */
    STRIP_QUERY,
}

/**
 * Per-header policy for what an insights record may contain.
 *
 * ```kotlin
 * HeaderLogging.defaults
 *     .with("x-internal-token", HeaderAction.REDACT)   // exact name
 *     .with(Regex("^x-debug-.*"), HeaderAction.DROP)   // pattern
 *     .withDefault(HeaderAction.REDACT)                // everything unmatched
 * ```
 *
 * **The last matching rule wins**, so an application always overrides the defaults by adding a rule
 * after them. There is no wildcard syntax: a name is either an exact match or a [Regex]. A bespoke
 * `"x-*"` form would be a third pattern language whose semantics nobody could guess.
 *
 * Matching is case-insensitive — names are lower-cased before comparison, so write a [Regex] against a
 * lower-case name.
 *
 * Replaces an implementation that kept the first 20 characters of `Authorization`, which is not a
 * redaction: `Basic ` is six characters, so fourteen base64 characters survived — ten decoded bytes of
 * `user:password`, i.e. all of a short credential. It also threw on shorter values, and covered no
 * other header; `Cookie` and `Set-Cookie` were stored verbatim.
 */
class HeaderLogging private constructor(
    private val rules: List<Rule>,
    private val default: HeaderAction,
) {
    /** One matcher and the action it selects. */
    private sealed interface Rule {
        val action: HeaderAction

        fun matches(lowercaseName: String): Boolean

        data class Exact(val name: String, override val action: HeaderAction) : Rule {
            override fun matches(lowercaseName: String) = lowercaseName == name
        }

        data class Pattern(val regex: Regex, override val action: HeaderAction) : Rule {
            override fun matches(lowercaseName: String) = regex.matches(lowercaseName)
        }
    }

    companion object {
        /** Replaces a redacted value. Fixed, so it cannot be mistaken for content. */
        const val REDACTED = "***redacted***"

        /**
         * Header names that always carry credentials.
         *
         * `authorization` covers JWTs and funktor API keys alike — `apiKeyCaller` reads
         * `Authorization: Bearer <token>`, the same header as `jwtCaller`. `authentication` is not a
         * real header, but is a common enough misspelling to be worth catching.
         */
        private val knownSensitive = listOf(
            "authorization",
            "authentication",
            "proxy-authorization",
            "cookie",
            "set-cookie",
            "x-api-key",
            "api-key",
            "x-auth-token",
            "x-csrf-token",
            "x-xsrf-token",
        )

        /**
         * Catches credential-bearing headers nobody listed — `X-Amz-Security-Token`, an app's own
         * service token.
         *
         * It can only ever redact MORE than intended, never less, and any rule added afterwards wins.
         *
         * `cookie` is in the alternation as well as the exact list because the exact list cannot cover
         * `cookie2` / `set-cookie2` (RFC 2965) or any other variant — without it those fail open.
         * `signature` covers `x-signature`, `stripe-signature`, `x-hub-signature-256` and friends, which
         * are request HMACs.
         *
         * `api[-_]?key` and not `api-?key` because this pattern is reused for QUERY PARAMETER names by
         * [applyToQueryParams]. Header names are kebab-case; query parameters are conventionally
         * snake_case, so `?api_key=` — the very example that KDoc gives — was not matched.
         */
        private val sensitiveByName = Regex(
            ".*(token|secret|password|passwd|credential|session|auth|api[-_]?key|cookie|signature).*"
        )

        /**
         * Redact what is known or looks sensitive, log the rest.
         *
         * The default for unmatched headers is [HeaderAction.LOG] deliberately: an insights record's
         * value for debugging lies largely in the headers nobody thought to enumerate. The pattern rule
         * is what stops that from failing open.
         */
        val defaults: HeaderLogging
            get() = HeaderLogging(
                // The heuristic goes FIRST so the explicit list is not shadowed by it. Both say REDACT
                // today, so the order is invisible — until someone changes the pattern's action, at
                // which point the wrong order would silently un-redact `authorization`, which the
                // pattern also matches via `.*auth.*`. Caught by mutation-testing this file.
                rules = listOf(Rule.Pattern(sensitiveByName, HeaderAction.REDACT)) +
                        knownSensitive.map { Rule.Exact(it, HeaderAction.REDACT) } +
                        // after the deny-list, so STRIP_QUERY wins for these
                        // `referer` is where a token is echoed BACK; `location` is where one is MINTED —
                        // an OAuth or magic-link flow answers `302 Location: /cb?code=<grant>`, and the
                        // record of that redirect would otherwise BE the credential. Neither name matches
                        // the alternation above, so both were stored verbatim.
                        listOf(
                            Rule.Exact("referer", HeaderAction.STRIP_QUERY),
                            Rule.Exact("location", HeaderAction.STRIP_QUERY),
                            Rule.Exact("content-location", HeaderAction.STRIP_QUERY),
                        ),
                default = HeaderAction.LOG,
            )

        /** Only explicitly logged headers keep their value. For records that leave the machine. */
        val strict: HeaderLogging get() = defaults.withDefault(HeaderAction.REDACT)
    }

    /** Adds an exact-name rule. [name] is lower-cased. */
    fun with(name: String, action: HeaderAction): HeaderLogging =
        HeaderLogging(rules + Rule.Exact(name.lowercase(), action), default)

    /** Adds a pattern rule. [pattern] is matched against the lower-cased header name. */
    fun with(pattern: Regex, action: HeaderAction): HeaderLogging =
        HeaderLogging(rules + Rule.Pattern(pattern, action), default)

    /** Sets what happens to headers no rule matches. */
    fun withDefault(action: HeaderAction): HeaderLogging = HeaderLogging(rules, action)

    /** The action for [name] — the last matching rule, or [default] when none match. */
    fun actionFor(name: String): HeaderAction {
        val lower = name.lowercase()

        return rules.lastOrNull { it.matches(lower) }?.action ?: default
    }

    /** Applies the policy to [headers]: dropped names disappear, redacted ones keep only their name. */
    fun applyTo(headers: Map<String, List<String>>): Map<String, List<String>> =
        headers.mapNotNull { (name, values) ->
            when (actionFor(name)) {
                HeaderAction.LOG -> name to values
                HeaderAction.REDACT -> name to values.map { REDACTED }
                HeaderAction.STRIP_QUERY -> name to values.map { it.substringBefore('?') }
                HeaderAction.DROP -> null
            }
        }.toMap()

    /**
     * Applies the same name-based policy to query PARAMETERS.
     *
     * Headers were never the only place a credential travels: `?token=`, `?code=`, `?api_key=` and
     * presigned-URL signatures all ride the query string, and before this they were stored verbatim.
     * The parameter name is matched by exactly the rules that match a header name, so a policy
     * extension covers both at once.
     *
     * [HeaderAction.STRIP_QUERY] has no meaning for a bare value and is treated as [HeaderAction.REDACT].
     */
    fun applyToQueryParams(params: Map<String, List<String>>): Map<String, List<String>> =
        params.mapNotNull { (name, values) ->
            when (actionFor(name)) {
                HeaderAction.LOG -> name to values
                HeaderAction.DROP -> null
                else -> name to values.map { REDACTED }
            }
        }.toMap()
}
