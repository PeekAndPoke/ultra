package io.peekandpoke.funktor.saas.domain

import io.peekandpoke.ultra.common.isSlug

/**
 * Validation and normalization rules for organisation / branch slugs.
 *
 * A slug is the stable, human-facing tenant key — it backs the unique index, `findBySlug`, the
 * ensure-default-org hook, and (later) realm auto-join. It is also intended to work as a **DNS
 * subdomain label** (e.g. `acme.b2b.example.com`) that can auto-select the tenant on login, so it
 * must be a valid, lowercase DNS label.
 *
 * The rules live here in commonMain so the UI (`accepts(validSlug())`) and the server enforce the
 * exact same contract — validation must never drift between the two.
 */
object Slugs {

    /** Minimum length. */
    const val MIN_LENGTH = 2

    /** Maximum length — the DNS label limit (RFC 1035). */
    const val MAX_LENGTH = 63

    /**
     * Labels that would collide with platform infrastructure subdomains and therefore may not be
     * used as a slug. Extend as the platform grows (kept deliberately small and explicit).
     */
    val RESERVED = setOf(
        "www", "api", "app", "admin", "ops", "b2b", "b2b2c",
        "mail", "smtp", "ftp", "static", "cdn", "assets", "auth", "login",
    )

    /** Canonical form of a slug: trimmed and lower-cased. Storage always uses this. */
    fun normalize(raw: String): String = raw.trim().lowercase()

    /** `true` when [slug] (expected already [normalize]d) is a valid, subdomain-safe label. */
    fun isValid(slug: String): Boolean = validationError(slug) == null

    /**
     * Returns a human-readable reason why [slug] is not a valid subdomain-safe label, or `null`
     * when it is valid. Expects an already-[normalize]d value so it validates exactly what will be
     * stored.
     */
    fun validationError(slug: String): String? = when {
        slug.isEmpty() -> "must not be blank"
        slug.length < MIN_LENGTH -> "must be at least $MIN_LENGTH characters"
        slug.length > MAX_LENGTH -> "must be at most $MAX_LENGTH characters"
        !slug.isSlug() ->
            "must contain only lowercase letters, digits and hyphens, " +
                "and start and end with a letter or digit"
        slug in RESERVED -> "'$slug' is a reserved name"
        else -> null
    }
}

/**
 * Canonical form of an organisation slug: trimmed and lower-cased. Delegates to [Slugs.normalize];
 * kept as a top-level alias so existing call sites need no change.
 */
fun normalizeSlug(raw: String): String = Slugs.normalize(raw)
