package io.peekandpoke.ultra.i18n

import io.peekandpoke.ultra.i18n.model.splitLocaleTag

/**
 * A locale = language + optional region (BCP-47 shape: `de`, `de-DE`, `de-CH`).
 *
 * [language] is always stored lower-case and [region] (when present) upper-case — normalization
 * happens at construction (via the companion [invoke]) as well as in [parse], so `Locale("DE")` and
 * `Locale("de")` are equal and both match a `de`-keyed catalog.
 *
 * **v1 scope (see the i18n plan, D11): language + region only.** Script and variant subtags — e.g.
 * `zh-Hant`, `sr-Latn`, `de-DE-1996` — are NOT modelled; the second subtag is always treated as a
 * region and upper-cased, so such tags are mis-parsed (`zh-Hant` -> `zh-HANT`). Do not rely on them
 * until script support is added.
 */
@ConsistentCopyVisibility
data class Locale private constructor(
    val language: String,
    val region: String?,
) {
    companion object {
        /** Creates a normalized [Locale] (language lower-cased, region upper-cased, blanks dropped). */
        operator fun invoke(language: String, region: String? = null): Locale =
            Locale(
                language = language.trim().lowercase(),
                region = region?.trim()?.takeIf { it.isNotBlank() }?.uppercase(),
            )

        /**
         * Parses a tag like `"de"` or `"de-CH"` (separator `-` or `_`), normalizing case.
         *
         * The grammar lives in [splitLocaleTag], shared with the build-time catalog tooling so a baked
         * catalog key cannot be normalized differently from the lookup that reads it.
         */
        fun parse(tag: String): Locale {
            val (language, region) = splitLocaleTag(tag)
            // Call invoke by name (not `Locale(...)`, which would bind to the private constructor and
            // skip normalization).
            return invoke(language, region)
        }
    }

    /** The base locale without the region (`de-CH` -> `de`); a base locale returns itself. */
    val base: Locale get() = if (region == null) this else Locale(language)

    /** The BCP-47 tag, e.g. `de` or `de-CH`. */
    val tag: String get() = if (region == null) language else "$language-$region"

    override fun toString(): String = tag
}
