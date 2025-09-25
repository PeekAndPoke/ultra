package de.peekandpoke.kraft.forms

import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.components.value
import de.peekandpoke.kraft.vdom.VDom

@DslMarker
annotation class KraftFormsDsl

@DslMarker
annotation class KraftFormsRuleDsl

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

@KraftFormsDsl
fun VDom.formController() = value { this.component.formController() }

@KraftFormsDsl
fun VDom.formObserver() = value { this.component.formObserver() }
