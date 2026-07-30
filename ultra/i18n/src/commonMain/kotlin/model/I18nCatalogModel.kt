package io.peekandpoke.ultra.i18n.model

/*
 * This directory is SOURCE-INCLUDED INTO buildSrc (`buildSrc/build.gradle.kts`), so it compiles twice:
 * once as part of this module and once inside buildSrc. One rule follows, and breaking it fails the
 * buildSrc compile before any project builds:
 *
 *   Reference nothing outside this directory — not even the rest of `ultra:i18n`. Files here may be
 *   imported FROM the module (`Locale` and `MessageResolver` both do), never the other way round.
 *
 * There is NO language-version restriction. There was until 2026-07-30, when buildSrc still used the
 * `kotlin-dsl` plugin and therefore compiled with the Kotlin embedded in Gradle at language version 1.8
 * — which is why `LocaleCatalog` carries a String tag rather than a `Locale`. buildSrc now compiles with
 * the project's own Kotlin, so that shape is legacy rather than necessary.
 *
 * Why this lives here at all: the build tooling that generates Kotlin accessors and the `ultra:codegen`
 * generator that will emit TypeScript both need this vocabulary, and `:tooling` is not published so it
 * cannot be the shared home.
 */

/**
 * One locale's catalog in the shape the generators consume: flat, dotted keys mapped to raw templates,
 * with plural forms still carrying their `_one` / `_other` suffix.
 *
 * [localeTag] is normalized ([normalizeLocaleTag]) so baked keys match runtime lookups. It is a String
 * rather than a `Locale` for historical reasons only — see the note at the top of this file.
 * [entries] iterates in source order, so generated output is deterministic.
 */
data class LocaleCatalog(
    val localeTag: String,
    val entries: Map<String, String>,
)

/**
 * Splits a BCP-47-ish tag into language + optional region, normalizing case: language lower, region
 * upper, separator `-` or `_`. `de_ch` -> `de` to `CH`.
 *
 * The single implementation of the tag grammar — `Locale.parse` and [normalizeLocaleTag] both go
 * through it, so a runtime lookup and a baked catalog key cannot disagree.
 */
fun splitLocaleTag(tag: String): Pair<String, String?> {
    val parts = tag.trim().split('-', '_', limit = 2)
    val language = parts[0].lowercase()
    val region = parts.getOrNull(1)?.takeIf { it.isNotBlank() }?.uppercase()
    return language to region
}

/** The normalized tag for [tag], identical to what `Locale.tag` would render. */
fun normalizeLocaleTag(tag: String): String {
    val (language, region) = splitLocaleTag(tag)
    return if (region == null) language else "$language-$region"
}

/**
 * Every recognised plural-form suffix; a catalog key carries one as `<key>_<suffix>` (D7).
 *
 * Kept in step with the `PluralCategory` enum by `PluralSuffixParitySpec` — it cannot be derived from
 * the enum here, since that is outside this directory.
 */
val pluralSuffixes: Set<String> = setOf("zero", "one", "two", "few", "many", "other")

/**
 * Splits a trailing plural suffix off [key]: `items_one` -> `items` to `true`; a key whose last `_`
 * segment is not a category — or which has none — comes back unchanged and `false`.
 */
fun splitPluralSuffix(key: String): Pair<String, Boolean> {
    val lastSep = key.lastIndexOf('_')
    if (lastSep <= 0) return key to false
    val suffix = key.substring(lastSep + 1)
    return if (suffix in pluralSuffixes) key.substring(0, lastSep) to true else key to false
}

/** [key] without its trailing plural suffix, so every form of a message maps to one base key. */
fun basePluralKey(key: String): String = splitPluralSuffix(key).first

/**
 * The `{{name}}` placeholder syntax (D7), declared once for every stage that touches it.
 *
 * `MessageResolver` substitutes it, the build-time catalog checker compares it across locales, and the
 * generators turn it into accessor parameters. Widening the pattern in only one of those would make the
 * checker blind to placeholders the resolver still substitutes — which is the error the checker exists
 * to raise.
 */
object I18nPlaceholders {

    /** A single bounded character class, linear (no ReDoS). */
    val pattern = Regex("""\{\{([a-zA-Z0-9_-]+)\}\}""")

    /** Placeholder names in [template], in first-seen order, duplicates kept. */
    fun namesIn(template: String): List<String> =
        pattern.findAll(template).map { it.groupValues[1] }.toList()
}
