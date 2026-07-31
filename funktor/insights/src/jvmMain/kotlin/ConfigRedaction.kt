package io.peekandpoke.funktor.insights

/**
 * **INTERIM — but NOT deletable when `Redacted<T>` is adopted. Only when JACKSON is gone.**
 *
 * `Redacted<T>` landed on the config classes on 2026-07-31 and this object is still load-bearing.
 * Measured, not assumed: with this redaction removed, a real record contains
 *
 * ```json
 * "signingKey": { "value": "ka2fEBWhmjPFpaPhg5Iir6tAX1COT0lG…" }
 * ```
 *
 * `Redacted<T>` teaches Slumber and kotlinx; **Jackson knows nothing about it** and serialises the
 * wrapper as an ordinary object, so the secret survives one level deeper. `AppConfigCollector` writes
 * through Jackson (`InsightsMapper`), so this is what keeps the key out of records today.
 *
 * Delete it at **stage 3** — when the insights write path leaves Jackson — and not before.
 *
 * Tracked in `.claude/tasks/20260731-redacted-and-jackson-removal.md`, stage 3.
 *
 * Why it exists: `AppConfigCollector` serialises the entire `AppConfig`, and the JWT signing key, the
 * CSRF secret and (in the demo) the database password are written into every record verbatim. Confirmed
 * by reading a real record on 2026-07-31, not inferred. `@JsonIgnore` protects four fields elsewhere and
 * none of these — which is the whole argument for replacing annotations with a type.
 *
 * Deliberately a **broader** pattern than [HeaderLogging]'s: a bare `key` is a false positive among HTTP
 * header names (`x-request-key-hint`) but is the dominant spelling in configuration — `signingKey`,
 * `apiKey`, `privateKey`, `keyStore`. Over-redacting a config value costs a debugging detail; under
 * redacting one costs the signing key.
 */
object ConfigRedaction {

    const val REDACTED = "***redacted***"

    /**
     * Matched against the lower-cased property name, anywhere in the serialised config tree.
     *
     * `HeaderLogging`'s pattern is not reused because it is tuned for header names: it matches
     * `api[-_]?key` but not a bare `key`, so `signingKey` — the most dangerous field in the tree —
     * slips straight through it. Verified against the real value.
     */
    private val sensitiveName = Regex(
        ".*(key|secret|password|passwd|passphrase|credential|token|salt|signature|connectionstring|dsn).*"
    )

    /** True when a property called [name] must never appear in a record. */
    fun isSensitive(name: String): Boolean = sensitiveName.matches(name.lowercase())

    /**
     * Walks [node] and replaces every value whose property name looks sensitive.
     *
     * A sensitive name redacts the **whole subtree** beneath it, not just a scalar — `aws: { accessKey,
     * secretKey }` under a key called `credentials` must not survive by being one level deeper.
     */
    fun redact(node: Any?): Any? = when (node) {
        is Map<*, *> -> node.entries.associate { (k, v) ->
            k to when {
                k is String && isSensitive(k) -> REDACTED
                else -> redact(v)
            }
        }

        is Iterable<*> -> node.map { redact(it) }

        else -> node
    }
}
