package de.peekandpoke.ultra.meta.model

import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier

class MMethod(
    model: Model,
    executableElement: ExecutableElement
) : MBase(model) {

    val simpleName: String = executableElement.simpleName.toString()

    val isPublic = executableElement.modifiers.contains(Modifier.PUBLIC)
}
