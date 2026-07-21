package io.peekandpoke.kraft.i18n

import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.getAttributeRecursive
import io.peekandpoke.kraft.i18n.generated.KraftFormsCatalog
import io.peekandpoke.ultra.i18n.I18n
import io.peekandpoke.ultra.i18n.I18nFormat
import io.peekandpoke.ultra.i18n.I18nTranslate
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

/** Installs kraft's built-in form-validation catalog (D3). Apps building their own [I18n] should call this. */
fun I18n.Builder.installKraftForms(): I18n.Builder = install(KraftFormsCatalog)

/**
 * The app-wide [I18nController], resolved from the component tree's attributes. The kraft app builder
 * registers a default (via `I18nController.default()`), so this never throws for a mounted component;
 * register a real one with `kraftApp { i18n(I18nController.create(locale, fallback) { install(appCatalog) }) }`.
 */
val Component<*>.i18nCtrl: I18nController get() = getAttributeRecursive(I18nController.key)

/**
 * Delegate for the open translation surface — `private val t by Translations` — re-rendering the
 * component whenever the language switches. Then `t.forms.minLength(count = 5)` etc. (the namespace
 * accessors are generated per module by the i18n codegen).
 */
object Translations {
    operator fun provideDelegate(
        thisRef: Component<*>,
        property: KProperty<*>,
    ): ReadOnlyProperty<Any?, I18nTranslate> =
        thisRef.subscribingTo(thisRef.i18nCtrl.translateStream)
}

/** Delegate for the closed formatting surface — `private val format by Formatting`. */
object Formatting {
    operator fun provideDelegate(
        thisRef: Component<*>,
        property: KProperty<*>,
    ): ReadOnlyProperty<Any?, I18nFormat> =
        thisRef.subscribingTo(thisRef.i18nCtrl.formatStream)
}
