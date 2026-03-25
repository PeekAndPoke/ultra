package io.peekandpoke.kraft.forms

import io.peekandpoke.kraft.components.Component
import io.peekandpoke.kraft.components.Ctx

abstract class FormComponent<PROPS>(ctx: Ctx<PROPS>) : Component<PROPS>(ctx) {
    val formController by lazy { _root_ide_package_.io.peekandpoke.kraft.forms.FormController(this) }
}
