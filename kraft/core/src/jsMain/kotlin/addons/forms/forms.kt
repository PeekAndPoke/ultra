package de.peekandpoke.kraft.addons.forms

import de.peekandpoke.kraft.components.Component

@DslMarker
annotation class KraftFormsDsl

@DslMarker
annotation class KraftFormsSettingDsl

@KraftFormsDsl
fun <T> Component<T>.formController(): FormController {
    return FormController(component = this, stopEvents = true)
}

@KraftFormsDsl
fun <T> Component<T>.formObserver(): FormController {
    return FormController(component = this, stopEvents = false)
}
