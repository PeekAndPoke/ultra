package de.peekandpoke.ultra.mutator.meta.rendering

import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName
import de.peekandpoke.ultra.meta.KotlinPrinter
import de.peekandpoke.ultra.meta.ProcessorUtils
import de.peekandpoke.ultra.meta.model.MVariable

class ListAndSetPropertyRenderer(
    ctx: ProcessorUtils.Context,
    root: PropertyRenderers
) : CollectionPropertyRendererBase(ctx, root) {

    private val supported = listOf(
        "java.util.List",
        "java.util.Set"
    )

    override fun canHandle(type: TypeName) = type is ParameterizedTypeName
            // is the type supported?
            && supported.contains(type.rawType.fqn)
            // and the contained type must be supported as well
            && type.typeArguments.all { root.canHandle(it) }

    override fun KotlinPrinter.renderProperty(variable: MVariable) {

        val type = variable.typeName
        val name = variable.simpleName

        renderVariableComment(variable)

        internalRenderProperty(name, (type as ParameterizedTypeName).typeArguments[0])
    }

    override fun KotlinPrinter.renderForwardMapper(type: TypeName, depth: Int) {

        internalRenderForwardMapper((type as ParameterizedTypeName).typeArguments[0], depth)
    }

    override fun KotlinPrinter.renderBackwardMapper(type: TypeName, depth: Int) {

        internalRenderBackwardMapper(depth)
    }
}
