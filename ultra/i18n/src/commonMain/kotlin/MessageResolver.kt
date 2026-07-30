package io.peekandpoke.ultra.i18n

import io.peekandpoke.ultra.i18n.model.I18nPlaceholders

/**
 * Resolves a message [key] to text by walking two axes (see the i18n plan, D11):
 *  - the **locale chain** outer, most-specific first: `locale -> locale.base -> fallback -> fallback.base`
 *  - **catalog precedence** inner: [catalogs] are ordered highest-precedence first (app before framework, D3)
 *
 * The first `(locale, catalog)` hit wins — so a more specific locale always beats a less specific one,
 * and within the same locale the app catalog beats the framework. Placeholders are `{{name}}` (D7) and
 * are substituted in a **single pass**: a substituted value is never re-scanned, so a value that itself
 * contains `{{...}}` can never be re-interpreted as a placeholder (guards against cross-argument
 * injection — see `MessageResolverSpec`).
 */
class MessageResolver(
    val locale: Locale,
    val fallback: Locale,
    /** Highest precedence first (app before framework). */
    private val catalogs: List<I18nCatalog>,
) {
    /** The locale fallback chain, most-specific first, de-duplicated. */
    val chain: List<Locale> = listOf(locale, locale.base, fallback, fallback.base).distinct()

    /**
     * Resolves [key], substituting [args] into `{{...}}` placeholders. When no catalog in the chain
     * has the key, the [key] itself is returned as a visible miss marker.
     */
    fun resolve(key: String, args: Map<String, Any?> = emptyMap()): String {
        val template = find(key) ?: return key
        return substitute(template, args)
    }

    /**
     * Resolves a plural [baseKey] for [count]: looks up `"${baseKey}_${category}"`, falling back to
     * the `_other` form, then to [baseKey] itself. `count` is exposed as the `{{count}}` placeholder.
     *
     * NOTE (D9): the category is derived from the requested [locale], not the locale the template was
     * actually found in. This is harmless while the rule is locale-independent (`count == 1` -> one),
     * but once CLDR plural rules land the category must come from the resolved template's locale.
     */
    fun resolvePlural(baseKey: String, count: Int, args: Map<String, Any?> = emptyMap()): String {
        val category = pluralCategoryFor(count, locale)
        val template = find("${baseKey}_${category.suffix}")
            ?: find("${baseKey}_${PluralCategory.OTHER.suffix}")
            ?: return baseKey
        return substitute(template, args + ("count" to count))
    }

    private fun find(key: String): String? {
        for (loc in chain) {
            for (catalog in catalogs) {
                catalog.template(key, loc)?.let { return it }
            }
        }
        return null
    }

    /** Single-pass substitution: scans [template] once, never re-scanning substituted values. */
    private fun substitute(template: String, args: Map<String, Any?>): String {
        if (args.isEmpty()) return template
        return I18nPlaceholders.pattern.replace(template) { match ->
            val name = match.groupValues[1]
            when {
                args.containsKey(name) -> args[name]?.toString() ?: ""
                else -> match.value // leave unknown placeholders untouched
            }
        }
    }
}
