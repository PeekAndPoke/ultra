package de.peekandpoke.ultra.mutator.meta.rendering

import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName
import de.peekandpoke.ultra.meta.KotlinPrinter
import de.peekandpoke.ultra.meta.ProcessorUtils

interface PropertyRenderer : ProcessorUtils {

    /**
     * Returns 'true' when the renderer can handle the given VariableElement
     */
    fun canHandle(type: TypeName): Boolean

    /**
     * Renders code-blocks for a property
     */
    fun KotlinPrinter.renderProperty(type: TypeName, name: String)

    fun KotlinPrinter.renderForwardMapper(type: TypeName, depth: Int)

    fun KotlinPrinter.renderBackwardMapper(type: TypeName, depth: Int)

    val Int.asParam get() = "it$this"

    val Int.asOnModify get() = "on$this"
}

/**
 * A collection of CodeRenderers
 *
 * canHandle() returns 'true' when one of the children returns true for the given VariableElement.
 * .
 * render() returns the code by calling render on the first child that can handle the given VariableElement.
 */
class PropertyRenderers(

    override val ctx: ProcessorUtils.Context,
    private val fallback: PropertyRenderer,
    private val provider: (PropertyRenderers) -> List<PropertyRenderer>

) : PropertyRenderer {

    /**
     * All property renderers
     */
    private val children by lazy { provider(this) }

    /**
     * Internal lookup from [TypeName] to [PropertyRenderer]
     */
    private val cache = mutableMapOf<TypeName, PropertyRenderer>()

    /**
     * We can always handle any type, since we have a fallback
     */
    override fun canHandle(type: TypeName): Boolean = true

    /**
     * Returns the code for the first matching child renderer
     */
    override fun KotlinPrinter.renderProperty(type: TypeName, name: String) =
        match(type).run { renderProperty(type, name) }

    /**
     * TODO: comment
     */
    override fun KotlinPrinter.renderForwardMapper(type: TypeName, depth: Int) =
        match(type).run { renderForwardMapper(type, depth) }

    /**
     * TODO: comment
     */
    override fun KotlinPrinter.renderBackwardMapper(type: TypeName, depth: Int) =
        match(type).run { renderBackwardMapper(type, depth) }

    /**
     * Get the [PropertyRenderer] for the given [TypeName]
     */
    private fun match(type: TypeName) = cache.getOrPut(type) {
        children.firstOrNull { it.canHandle(type) } ?: fallback
    }
}

/**
 * Renderer for primitive types and Strings
 */
class PureGetterSetterRenderer(
    override val ctx: ProcessorUtils.Context
) : PropertyRenderer {

    override fun canHandle(type: TypeName) = true

    override fun KotlinPrinter.renderProperty(type: TypeName, name: String) {

        if (!type.isPrimitiveType && !type.isStringType && !type.isAnyType) {
            block(
                "// Currently there is no better way to mutate a '$type' ... sorry!"
            )
        }

        block(
            """
                var $name
                    get() = getResult().$name
                    set(v) = modify(getResult()::$name, getResult().$name, v)
            """.trimIndent()
        )

        newline()
    }

    override fun KotlinPrinter.renderForwardMapper(type: TypeName, depth: Int) {
        append(depth.asParam)
    }

    override fun KotlinPrinter.renderBackwardMapper(type: TypeName, depth: Int) {
        append(depth.asParam)
    }
}

abstract class CollectionPropertyRendererBase(
    override val ctx: ProcessorUtils.Context,
    protected val root: PropertyRenderers
) : PropertyRenderer {

    protected fun KotlinPrinter.internalRenderProperty(name: String, typeParam: TypeName) {

        line("val $name by lazy {").indent {
            line("getResult().$name.mutator(").indent {
                line("{ modify(getResult()::$name, getResult().$name, it) },")

                append("{ ${1.asParam} -> ")
                root.run { renderBackwardMapper(typeParam, 1) }
                line(" },")

                line("{ ${1.asParam}, ${1.asOnModify} -> ").indent {
                    root.run { renderForwardMapper(typeParam, 1) }
                    newline()
                }
                line("}")
            }
            line(")")
        }
        line("}")
    }

    fun KotlinPrinter.internalRenderForwardMapper(type: TypeName, depth: Int) {

        val plus1 = depth + 1

        append("${depth.asParam}.mutator(${depth.asOnModify}, { ${plus1.asParam} -> ")
        root.run { renderBackwardMapper(type, plus1) }
        line(" }) { ${plus1.asParam}, ${plus1.asOnModify} -> ").indent {
            root.run { renderForwardMapper(type, plus1) }
            newline()
        }
        append("}")
    }

    fun KotlinPrinter.internalRenderBackwardMapper(depth: Int) {
        append("${depth.asParam}.getResult()")
    }
}

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

    override fun KotlinPrinter.renderProperty(type: TypeName, name: String) {

        internalRenderProperty(name, (type as ParameterizedTypeName).typeArguments[0])
    }

    override fun KotlinPrinter.renderForwardMapper(type: TypeName, depth: Int) {

        internalRenderForwardMapper((type as ParameterizedTypeName).typeArguments[0], depth)
    }

    override fun KotlinPrinter.renderBackwardMapper(type: TypeName, depth: Int) {

        internalRenderBackwardMapper(depth)
    }
}

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

    override fun KotlinPrinter.renderProperty(type: TypeName, name: String) {

        internalRenderProperty(name, (type as ParameterizedTypeName).typeArguments[1])
    }

    override fun KotlinPrinter.renderForwardMapper(type: TypeName, depth: Int) {

        internalRenderForwardMapper((type as ParameterizedTypeName).typeArguments[1], depth)
    }

    override fun KotlinPrinter.renderBackwardMapper(type: TypeName, depth: Int) {

        return internalRenderBackwardMapper(depth)
    }
}

/**
 * Here we handle non parameterized data classes
 */
class DataClassPropertyRenderer(
    override val ctx: ProcessorUtils.Context
) : PropertyRenderer {

    // TODO: check if the type has a "copy" method
    override fun canHandle(type: TypeName) =
        // exclude blank name (probably a generic type like T)
        type.packageName.isNotEmpty() &&
                // we also exclude some packages completely
                !type.isBlackListed

    override fun KotlinPrinter.renderProperty(type: TypeName, name: String) {

        val nullable = if (type.isNullable) "?" else ""

        block(
            """
                val $name by lazy { 
                    getResult().$name$nullable.${type.import("mutator")} { modify(getResult()::$name, getResult().$name, it) } 
                }
    
            """.trimIndent()
        )
    }

    override fun KotlinPrinter.renderForwardMapper(type: TypeName, depth: Int) {

        append("${depth.asParam}.${type.import("mutator")}(${depth.asOnModify})")
    }

    override fun KotlinPrinter.renderBackwardMapper(type: TypeName, depth: Int) {

        append("${depth.asParam}.getResult()")
    }
}
