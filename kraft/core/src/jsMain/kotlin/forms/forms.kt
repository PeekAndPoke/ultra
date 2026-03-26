package io.peekandpoke.kraft.forms

import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.value
import io.peekandpoke.kraft.vdom.VDom

/** DSL marker for Kraft forms builder scope. */
@DslMarker
annotation class KraftFormsDsl

/** DSL marker for Kraft forms validation rule scope. */
@DslMarker
annotation class KraftFormsRuleDsl

/** DSL marker for Kraft forms settings scope. */
@DslMarker
annotation class KraftFormsSettingDsl

/** Creates a [FormController] that stops form events from propagating. */
@KraftFormsDsl
fun <T> Component<T>.formController(): FormController {
    return FormController(component = this, stopEvents = true)
}

/** Creates a [FormController] that observes form events without stopping them. */
@KraftFormsDsl
fun <T> Component<T>.formObserver(): FormController {
    return FormController(component = this, stopEvents = false)
}

/** Creates a [FormController] as a remembered value in a VDom render scope. */
@KraftFormsDsl
fun VDom.formController() = value { this.component.formController() }

/** Creates a form observer as a remembered value in a VDom render scope. */
@KraftFormsDsl
fun VDom.formObserver() = value { this.component.formObserver() }
