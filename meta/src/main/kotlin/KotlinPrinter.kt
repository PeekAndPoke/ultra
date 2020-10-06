package de.peekandpoke.ultra.meta

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.TypeName
import de.peekandpoke.ultra.meta.model.MType

class KotlinPrinter(
    private val packageName: String,
    private val fileAnnotations: List<String> = listOf(),
    defaultImports: List<String> = listOf(),
    indentSize: Int = 4
) {

    class Imports(private val codePackageName: String, defaultImports: List<String>) {

        private val imports = defaultImports.toMutableSet()

        val all get() = imports.toSet()

        fun import(type: TypeName): String = when (type) {

            is ClassName -> type.toKotlin.run {

                val joinedSimpleNames = simpleNames.joinToString(".")

                // There is no need to import:
                // - classes in the same package as the generated class
                // - kotlin std lib classes
                if (packageName in listOf(codePackageName, "kotlin", "kotlin.collections")) {
                    return@run joinedSimpleNames
                }

                val possibleImport = "$packageName.${simpleNames[0]}"

                // We need to make sure that there are no conflicting imports
                val conflicts = imports.any {
                    it != possibleImport && it.split(".").last() == simpleNames.first()
                }

                return@run when (conflicts) {
                    true -> canonicalName

                    false -> {
                        imports.add(possibleImport)

                        joinedSimpleNames
                    }
                }
            }

            is ParameterizedTypeName -> {
                "${import(type.rawType)}<${type.typeArguments.joinToString(", ") { import(it) }}>"
            }

            else -> toString()
        }


        /**
         * Imports a top level function
         */
        fun import(type: TypeName, function: String): String {

            when (type) {
                is ClassName ->

                    // We only need to import top level function from other packages
                    if (type.packageName != codePackageName) {
                        imports.add("${type.packageName}.${function}")
                    }

                is ParameterizedTypeName ->
                    import(type.rawType, function)
            }

            return function
        }
    }

    /** The string builder used to generate the code */
    private val builder = StringBuilder()
    /** Accumulates all imports while generating the code */
    private val imports = Imports(packageName, defaultImports)

    /** Indentation */
    private val indentation = " ".repeat(indentSize)
    /** Current indentation */
    private var currentIndent = ""
    /** 'True' when a new line was just started */
    private var isNewLine = false
    /** System newline character */
    private val nl = System.lineSeparator()

    /**
     * Returns the resulting code
     */
    override fun toString(): String {

        return fileAnnotations.plus(
            listOf(
                "",
                "package $packageName",
                "",
                *imports.all.map { "import $it" }.toTypedArray(),
                ""
            )
        ).joinToString(nl) + nl + builder.toString()
    }

    /**
     * Generates a short name for the given [MType]
     *
     * Record the proper import for the type. Type arguments will be imported as wll.
     */
    fun MType.import(): String = typeName.import()

    /**
     * Imports a top level [function], returns the shortened name
     */
    fun MType.import(function: String) = typeName.import(function)

    /**
     * Generates a short name for the given [TypeName]
     *
     * Record the proper import for the type. Type arguments will be imported as wll.
     */
    fun TypeName.import(): String = imports.import(this)

    /**
     * Imports a top level [function], returns the shortened name
     */
    fun TypeName.import(function: String): String = imports.import(this, function)

    /**
     * Appends a string
     */
    fun append(str: String) = apply {

        if (isNewLine) {
            builder.append(currentIndent)
            isNewLine = false
        }

        builder.append(str)
    }

    /**
     * Appends a string followed by a newline
     */
    fun line(str: String) = apply {
        append(str)
        newline()
    }

    /**
     * Appends a block.
     *
     * Prepend a newline, when not currently at the beginning of a line.
     * Then appends the block.
     * Finishes with another newline.
     */
    fun block(block: String) = apply {

        if (!isNewLine) {
            builder.appendLine()
        }

        append(
            block.prependIndent(currentIndent).trimStart()
        )

        newline()
    }

    fun divider(pattern: String = "/", size: Int = 120) = apply {
        if (pattern.isNotEmpty()) {
            block(pattern.repeat(size / pattern.length))
        }
    }

    /**
     * Appends a string followed by a line break
     */
    fun newline() = apply {

        builder.appendLine()

        isNewLine = true
    }

    /**
     * Increases the indent for everything added by the [block]
     */
    fun indent(block: KotlinPrinter.() -> Unit) {

        val previousIndent = currentIndent

        currentIndent += indentation

        this.block()

        currentIndent = previousIndent
    }
}
