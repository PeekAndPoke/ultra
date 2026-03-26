package io.peekandpoke.kraft.forms

import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.Ctx

/**
 * Base component that automatically creates a [FormController] for managing child form fields.
 */
abstract class FormComponent<PROPS>(ctx: Ctx<PROPS>) : Component<PROPS>(ctx) {
    /** Lazily initialized form controller for this component. */
    val formController by lazy { _root_ide_package_.io.peekandpoke.kraft.forms.FormController(this) }
}
