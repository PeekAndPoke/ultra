package io.peekandpoke.funktor.saas.domain

/**
 * Canonical form of an organisation slug: trimmed and lower-cased.
 *
 * The slug is the stable, human-facing tenant key — it backs the unique index, `findBySlug`, the
 * ensure-default-org hook, and (later) realm auto-join. It must be normalized the same way at every
 * write so that `"Acme"`, `"acme"` and `" acme "` cannot become three distinct tenants.
 */
internal fun normalizeSlug(raw: String): String = raw.trim().lowercase()
