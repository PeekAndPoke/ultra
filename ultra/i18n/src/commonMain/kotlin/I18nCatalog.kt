package io.peekandpoke.ultra.i18n

/**
 * A source of message templates, keyed by fully-qualified key and [Locale].
 *
 * Catalogs are baked at build time and each module ships its own (see the i18n plan, D1/D2). Plural
 * forms are stored as separate keys with a `_one` / `_other` suffix (D7); category selection happens
 * in [MessageResolver], not here.
 */
interface I18nCatalog {
    /** The raw template for [key] in exactly [locale] (no fallback here), or `null` if absent. */
    fun template(key: String, locale: Locale): String?
}

/**
 * A simple in-memory [I18nCatalog] backed by `locale-tag -> (key -> template)`.
 *
 * Hand-written catalogs use this in tests; the codegen (build step S2) emits an equivalent `object`.
 */
class MapI18nCatalog(
    private val byLocale: Map<String, Map<String, String>>,
) : I18nCatalog {

    companion object {
        /** Builds a catalog from `locale-tag -> (key -> template)` pairs. */
        operator fun invoke(vararg entries: Pair<String, Map<String, String>>): MapI18nCatalog =
            MapI18nCatalog(entries.toMap())
    }

    override fun template(key: String, locale: Locale): String? =
        byLocale[locale.tag]?.get(key)
}
