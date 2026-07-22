package io.peekandpoke.ultra.i18n

/**
 * Closed formatting surface: dates, numbers, currency — locale-driven (see the i18n plan, D9/D11).
 *
 * Placeholder for now: real CLDR-backed formatting (and the `kotlinx-datetime` bump for locale dates)
 * is deferred to D9. The [locale] seam is here so downstream code can already route through
 * `i18n.format`, and the impl can be filled in without moving call sites.
 */
class I18nFormat(internal val i18n: I18n) {
    val locale: Locale get() = i18n.locale
}
