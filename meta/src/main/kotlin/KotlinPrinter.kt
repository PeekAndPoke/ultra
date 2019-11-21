package de.peekandpoke.ultra.meta

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName
import de.peekandpoke.ultra.meta.model.MType
import kotlin.math.max

class KotlinPrinter(
    private val packageName: String,
    private val fileAnnotations: List<String> = listOf(),
    imports: List<String>
) {

    private val builder = StringBuilder()

    private val imports = imports.toMutableSet()

    private var indent = ""
    private var isNewLine = false
    private val nl = System.lineSeparator()

    private val importCache = mutableMapOf<TypeName, String>()

    override fun toString(): String {

        return fileAnnotations.plus(
            listOf(
                "",
                "package $packageName",
                "",
                *imports.map { "import $it" }.toTypedArray(),
                ""
            )
        ).joinToString(nl) + nl + builder.toString()
    }

    fun MType.import(): String = typeName.import()

    fun TypeName.import(): String = importCache.getOrPut(this) {

        return when (this) {
            is ClassName -> toKotlin.run {

                // We do not import the things that are always visible to the compiler
                if (packageName !in listOf("kotlin", "kotlin.collections")) {
                    imports.add("$packageName.*")
                }

                // Take class nesting into account here
                simpleNames.joinToString(".")
            }

            is ParameterizedTypeName -> {
                "${rawType.import()}<${typeArguments.joinToString(", ") { it.import() }}>"
            }

            else -> toString()
        }
    }

    /**
     * Appends a string
     */
    fun append(str: String) = apply {

        if (isNewLine) {
            builder.append(indent)
            isNewLine = false
        }

        builder.append(str)
    }

    /**
     * Appends a string and a newline
     */
    fun appendLine(str: String) = apply {
        append(str)
        newline()
    }

    fun appendBlock(block: String) = apply {

        if (!isNewLine) {
            builder.appendln()
        }

        append(
            block.prependIndent(indent).trimStart()
        )

        newline()
    }

    /**
     * Appends a string followed by a line break
     */
    fun newline() = apply {

        builder.appendln()

        isNewLine = true
    }

    /**
     * Increases the indent for everything added by the [block]
     */
    fun indent(block: KotlinPrinter.() -> Unit) {

        indent += "    "

        this.block()

        indent = indent.substring(0, max(0, indent.length - 4))
    }
}
