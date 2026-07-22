package io.peekandpoke.ultra.i18n

/**
 * Root receiver for translation namespaces (the open surface — see the i18n plan, D2/D10).
 *
 * Module-generated namespaces hang off this as extension properties, e.g.
 * `val I18nTranslate.forms: KraftFormsI18n`. [i18n] is public so those generated accessors — which
 * live in other modules — can reach the resolver.
 */
class I18nTranslate(val i18n: I18n)
