package de.peekandpoke.kraft.addons.forms

import de.peekandpoke.kraft.components.Component
import de.peekandpoke.kraft.components.Ctx

abstract class FormComponent<PROPS>(ctx: Ctx<PROPS>) : Component<PROPS>(ctx) {
    val formController by lazy { FormController(this) }
}
