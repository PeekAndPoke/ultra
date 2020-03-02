package de.peekandpoke.ultra.mutator.meta.rendering

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName
import de.peekandpoke.ultra.meta.KotlinPrinter
import de.peekandpoke.ultra.meta.ProcessorUtils
import de.peekandpoke.ultra.meta.model.MVariable

class MapPropertyRenderer(
    ctx: ProcessorUtils.Context,
    root: PropertyRenderers
) : CollectionPropertyRendererBase(ctx, root) {

    private val supported = listOf(
        "java.util.Map"
    )

    override fun canHandle(type: TypeName) = type is ParameterizedTypeName
            // is the type supported ?
            && supported.contains(type.rawType.fqn)
            // The key of the map must be primitive or a string
            && type.typeArguments[0].let { it.isPrimitiveType || it.isStringType }
            // and the value part must be handled as well
            && root.canHandle(type.typeArguments[1])

    override fun KotlinPrinter.renderPropertyDeclaration(variable: MVariable) {

        val type = variable.typeName
        val name = variable.simpleName

        val keyParam = (type as ParameterizedTypeName).typeArguments[0]
        val valParam = (type as ParameterizedTypeName).typeArguments[1]

        // TODO: check the type cast
        val mutatorImported = (valParam as ClassName).toMutatorClassName().import()

        renderVariableComment(variable)

        block(
            """
                var $name: MapMutator<${keyParam.import()}, ${valParam.import()}, $mutatorImported>
                
            """.trimIndent()
        )
    }

    override fun KotlinPrinter.renderPropertyImplementation(variable: MVariable) {

        val type = variable.typeName
        val name = variable.simpleName

        renderVariableComment(variable)

        internalRenderProperty(name, (type as ParameterizedTypeName).typeArguments[1])
    }

    override fun KotlinPrinter.renderForwardMapper(type: TypeName, depth: Int) {

        internalRenderForwardMapper((type as ParameterizedTypeName).typeArguments[1], depth)
    }

    override fun KotlinPrinter.renderBackwardMapper(type: TypeName, depth: Int) {

        return internalRenderBackwardMapper(depth)
    }
}
