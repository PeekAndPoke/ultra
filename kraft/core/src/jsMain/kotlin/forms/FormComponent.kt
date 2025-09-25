package de.peekandpoke.kraft.forms

import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.components.Ctx

abstract class FormComponent<PROPS>(ctx: Ctx<PROPS>) : Component<PROPS>(ctx) {
    val formController by lazy { _root_ide_package_.de.peekandpoke.kraft.forms.FormController(this) }
}
