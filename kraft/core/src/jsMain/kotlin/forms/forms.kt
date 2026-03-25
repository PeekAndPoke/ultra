package io.peekandpoke.kraft.forms

import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.value
import io.peekandpoke.kraft.vdom.VDom

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
