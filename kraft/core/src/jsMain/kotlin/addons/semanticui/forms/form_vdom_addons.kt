package de.peekandpoke.kraft.addons.semanticui.forms

import de.peekandpoke.kraft.addons.forms.FormController
import de.peekandpoke.kraft.components.value
import de.peekandpoke.kraft.vdom.VDom

fun VDom.formController() = value { FormController(this.component!!) }
