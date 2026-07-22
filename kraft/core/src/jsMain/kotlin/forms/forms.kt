package io.peekandpoke.kraft.forms

import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.value
import io.peekandpoke.kraft.vdom.VDom

/** Creates a [FormController] that stops form events from propagating. */
fun <T> Component<T>.formController(): FormController {
    return FormController(component = this, stopEvents = true)
}

/** Creates a [FormController] that observes form events without stopping them. */
fun <T> Component<T>.formObserver(): FormController {
    return FormController(component = this, stopEvents = false)
}

/** Creates a [FormController] as a remembered value in a VDom render scope. */
fun VDom.formController() = value { this.component.formController() }

/** Creates a form observer as a remembered value in a VDom render scope. */
fun VDom.formObserver() = value { this.component.formObserver() }
