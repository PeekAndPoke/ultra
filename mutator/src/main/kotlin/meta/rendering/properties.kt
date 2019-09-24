package de.peekandpoke.ultra.mutator.meta.rendering

import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName
import de.peekandpoke.ultra.common.startsWithAny
import de.peekandpoke.ultra.meta.ProcessorUtils
import javax.annotation.processing.ProcessingEnvironment
import javax.lang.model.element.VariableElement

interface PropertyRenderer {

    /**
     * Returns 'true' when the renderer can handle the given VariableElement
     */
    fun canHandle(type: TypeName): Boolean

    /**
     * Returns a list of imports, that need to be added to the generated code
     */
    fun getImports(type: TypeName): List<String>

    /**
     * Renders all code-blocks for the given VariableElement
     */
    fun render(property: VariableElement): String

    fun renderForwardMapper(type: TypeName, depth: Int): String

    fun renderBackwardMapper(type: TypeName, depth: Int): String
}

/**
 * Abstract base class providing all the tools from the ProcessorUtils
 */
abstract class PropertyRendererBase(
    logPrefix: String,
    processingEnv: ProcessingEnvironment
) : RendererBase(logPrefix, processingEnv), PropertyRenderer {

    override fun getImports(type: TypeName) = listOf<String>()

    protected val Int.asParam get() = "it$this"

    protected val Int.asOnModify get() = "on$this"

    protected fun String.indent(amount: Int, pattern: String = "    ") = lineSequence()
        .mapIndexed { idx, str -> if (idx == 0) str else str.prependIndent(pattern.repeat(amount)) }
        .joinToString(System.lineSeparator())

    val TypeName.isBlackListed
        get() = fqn.startsWithAny(
            "java.",                // exclude java std lib
            "javax.",               // exclude javax std lib
            "javafx.",              // exclude javafx
            "kotlin.",              // exclude kotlin std lib
            "com.google.common."    // exclude google guava
        )
}

/**
 * A collection of CodeRenderers
 *
 * canHandle() returns 'true' when one of the children returns true for the given VariableElement.
 * .
 * render() returns the code by calling render on the first child that can handle the given VariableElement.
 */
class PropertyRenderers(

    logPrefix: String,
    env: ProcessingEnvironment,
    private val provider: (PropertyRenderers) -> List<PropertyRenderer>

) : PropertyRendererBase(logPrefix, env), ProcessorUtils {

    private val children by lazy { provider(this) }

    private val cache = mutableMapOf<String, PropertyRenderer?>()

    /**
     * Returns 'true' when one of the children returns true for the given VariableElement.
     */
    override fun canHandle(type: TypeName) = match(type) != null

    /**
     * Returns a list of imports, that need to be added to the generated code
     */
    override fun getImports(type: TypeName) = match(type)!!.getImports(type)

    /**
     * Returns the code for the first matching child renderer
     */
    override fun render(property: VariableElement) = match(property.asTypeName())!!.render(property)

    override fun renderForwardMapper(type: TypeName, depth: Int) = match(type)!!.renderForwardMapper(type, depth)

    override fun renderBackwardMapper(type: TypeName, depth: Int) = match(type)!!.renderBackwardMapper(type, depth)

    private fun match(type: TypeName) = cache.getOrPut(type.fqn) {
        children.firstOrNull { it.canHandle(type) }
    }
}

/**
 * Renderer for primitive types and Strings
 */
class PureGetterSetterRenderer(logPrefix: String, env: ProcessingEnvironment) :
    PropertyRendererBase(logPrefix, env) {

    override fun canHandle(type: TypeName) = true

    override fun render(property: VariableElement): String {

        val cls = property.asKotlinClassName() + if (property.isNullable) "?" else ""
        val prop = property.simpleName
        val type = property.asTypeName()

        val hint = when {
            type.isPrimitiveType || type.isStringType || type.isAnyType -> "\n"

            else -> "// Currently there is no better way to mutate a '$type' ... sorry!\n"
        }

        return """
            var $prop: $cls
                get() = getResult().$prop
                set(v) = modify(getResult()::$prop, getResult().$prop, v)
                $hint
        """.trimIndent()
    }

    override fun renderForwardMapper(type: TypeName, depth: Int): String {

        return """
            ${depth.asParam}
        """.trimIndent()
    }

    override fun renderBackwardMapper(type: TypeName, depth: Int): String {

        return """
            ${depth.asParam}
        """.trimIndent()
    }
}

class ListAndSetPropertyRenderer(
    private val root: PropertyRenderers,
    logPrefix: String,
    env: ProcessingEnvironment
) : PropertyRendererBase(logPrefix, env) {

    private val supported = listOf(
        "java.util.List",
        "java.util.Set"
    )

    override fun canHandle(type: TypeName) = type is ParameterizedTypeName
            // is the type supported?
            && supported.contains(type.rawType.fqn)
            // and the contained type must be supported as well
            && type.typeArguments.all { root.canHandle(it) }

    override fun render(property: VariableElement): String {

        val prop = property.simpleName
        val type = property.asTypeName() as ParameterizedTypeName
        val typeParam = type.typeArguments[0]

        return """
            val $prop by lazy {
                getResult().$prop.mutator(
                    { modify(getResult()::$prop, getResult().$prop, it) },
                    { ${1.asParam} -> ${root.renderBackwardMapper(typeParam, 1)} },
                    { ${1.asParam}, ${1.asOnModify} ->
                        ${root.renderForwardMapper(typeParam, 1).indent(6)}
                    }
                )
            }

        """.trimIndent()
    }

    override fun renderForwardMapper(type: TypeName, depth: Int): String {

        val typeParam = (type as ParameterizedTypeName).typeArguments[0]
        val plus1 = depth + 1

        return """
            ${depth.asParam}.mutator(${depth.asOnModify}, { ${plus1.asParam} -> ${root.renderBackwardMapper(
            typeParam,
            plus1
        )} }) { ${plus1.asParam}, ${plus1.asOnModify} ->
                ${root.renderForwardMapper(typeParam, plus1).indent(4)}
            }
        """.trimIndent()
    }

    override fun renderBackwardMapper(type: TypeName, depth: Int): String {

        return """
            ${depth.asParam}.getResult()
        """.trimIndent()
    }
}

class MapPropertyRenderer(
    private val root: PropertyRenderers,
    logPrefix: String,
    env: ProcessingEnvironment
) : PropertyRendererBase(logPrefix, env) {

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

    override fun render(property: VariableElement): String {

        val prop = property.simpleName
        val type = property.asTypeName() as ParameterizedTypeName

        val p1 = type.typeArguments[1]

        return """
            val $prop by lazy {
                getResult().$prop.mutator(
                    { modify(getResult()::$prop, getResult().$prop, it) },
                    { ${1.asParam} -> ${root.renderBackwardMapper(p1, 1)} },
                    { ${1.asParam}, ${1.asOnModify} ->
                        ${root.renderForwardMapper(p1, 1).indent(6)}
                    }
                )
            }

        """.trimIndent()
    }

    override fun renderForwardMapper(type: TypeName, depth: Int): String {

        val p1 = (type as ParameterizedTypeName).typeArguments[1]

        val plus1 = depth + 1

        return """
            ${depth.asParam}.mutator(${depth.asOnModify}, { ${plus1.asParam} -> ${root.renderBackwardMapper(
            p1,
            plus1
        )} }) { ${plus1.asParam}, ${plus1.asOnModify} ->
                ${root.renderForwardMapper(p1, plus1).indent(4)}
            }
        """.trimIndent()
    }

    override fun renderBackwardMapper(type: TypeName, depth: Int): String {

        return """
            ${depth.asParam}.getResult()
        """.trimIndent()
    }
}

/**
 * Here we handle non parameterized data classes
 */
class DataClassPropertyRenderer(logPrefix: String, env: ProcessingEnvironment) : PropertyRendererBase(logPrefix, env) {

    override fun canHandle(type: TypeName) =
        // exclude blank name (probably a generic type like T)
        type.packageName.isNotEmpty() &&
                // we also exclude some packages completely
                !type.isBlackListed

    // TODO: check if the type has a "copy" method

    override fun getImports(type: TypeName) = listOf("${type.packageName}.mutator")

    override fun render(property: VariableElement): String {

        val nullable = if (property.isNullable) "?" else ""

        val prop = property.simpleName

        return """
            val $prop by lazy { getResult().$prop$nullable.mutator { modify(getResult()::$prop, getResult().$prop, it) } }

        """.trimIndent()
    }

    override fun renderForwardMapper(type: TypeName, depth: Int): String {

        return """
            ${depth.asParam}.mutator(${depth.asOnModify})
        """.trimIndent()
    }

    override fun renderBackwardMapper(type: TypeName, depth: Int): String {

        return """
            ${depth.asParam}.getResult()
        """.trimIndent()
    }
}
