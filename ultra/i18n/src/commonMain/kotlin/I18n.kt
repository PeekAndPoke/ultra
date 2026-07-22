package io.peekandpoke.ultra.i18n

/**
 * Immutable i18n state: the selected [locale], the [fallback], and the installed catalogs. Changing
 * the language produces a **new** instance ([withLocale]) — the frontend stream (kraft, build step S4)
 * emits it so subscribed components re-render (see the i18n plan, D4/D10).
 *
 * Translation namespaces hang off [translate] (open extension surface); formatting off [format]
 * (closed framework surface) — D10.
 */
class I18n private constructor(
    val locale: Locale,
    val fallback: Locale,
    private val catalogs: List<I18nCatalog>,
) {
    companion object {
        /**
         * Builds an [I18n]. Catalogs installed later win (app after framework — D3).
         *
         * [fallback] is **required** — it is the always-present safety net at the end of the locale
         * chain (D4). It must be a concrete target language (e.g. `en`), NOT derived from [locale];
         * defaulting it to `locale.base` would drop the net for a regional locale (`de-CH -> de` with
         * no `en`), which is exactly the footgun D11's chain avoids.
         */
        operator fun invoke(
            locale: Locale,
            fallback: Locale,
            block: Builder.() -> Unit = {},
        ): I18n {
            val builder = Builder().apply(block)
            return I18n(locale, fallback, builder.build())
        }
    }

    private val resolver = MessageResolver(locale, fallback, catalogs)

    /** Open translation surface — module namespaces extend this (D2/D10). */
    val translate: I18nTranslate get() = I18nTranslate(this)

    /** Closed formatting surface (D9/D10). */
    val format: I18nFormat get() = I18nFormat(this)

    /** Resolves [key], substituting `{{...}}` [args]. */
    fun resolve(key: String, args: Map<String, Any?> = emptyMap()): String =
        resolver.resolve(key, args)

    /** Resolves a plural [baseKey] for [count]. */
    fun resolvePlural(baseKey: String, count: Int, args: Map<String, Any?> = emptyMap()): String =
        resolver.resolvePlural(baseKey, count, args)

    /** A new [I18n] with the same catalogs but a different [locale] (immutable snapshot — D10). */
    fun withLocale(locale: Locale): I18n = I18n(locale, fallback, catalogs)

    /** Collects catalogs in install order; [build] reverses so the last-installed wins (D3). */
    class Builder {
        private val installed = mutableListOf<I18nCatalog>()

        /** Installs a [catalog]. Later installs take precedence (app after framework). */
        fun install(catalog: I18nCatalog): Builder = apply { installed.add(catalog) }

        internal fun build(): List<I18nCatalog> = installed.asReversed().toList()
    }
}
