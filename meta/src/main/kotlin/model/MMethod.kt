package de.peekandpoke.ultra.meta.model

import de.peekandpoke.ultra.meta.ProcessorUtils
import javax.lang.model.element.ExecutableElement
import javax.lang.model.element.Modifier

data class MMethod(
    override val ctx: ProcessorUtils.Context,
    private val executableElement: ExecutableElement
) : ProcessorUtils {

    val simpleName: String = executableElement.simpleName.toString()

    val isPublic = executableElement.modifiers.contains(Modifier.PUBLIC)
}
