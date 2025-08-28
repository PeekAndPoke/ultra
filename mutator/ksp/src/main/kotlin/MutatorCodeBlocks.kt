package de.peekandpoke.ultra.ksp

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration

class MutatorCodeBlocks {

    private val blocks = mutableListOf<String>()

    fun isEmpty() = blocks.isEmpty()

    fun isNotEmpty() = !isEmpty()

    fun build() = blocks.joinToString("\n")

    fun wrapWithBackticks(str: String) = "`$str`"

    fun getClassName(cls: KSClassDeclaration): String {
        val names = mutableListOf<String>()
        var current: KSClassDeclaration? = cls

        while (current != null) {
            names.add(0, wrapWithBackticks(current.simpleName.asString()))
            current = current.parentDeclaration as? KSClassDeclaration
        }

        val fullName = names.joinToString(".")

        return fullName
    }

    fun getPropertyName(prop: KSPropertyDeclaration): String {
        return wrapWithBackticks(prop.simpleName.asString())
    }

    fun add(code: String) = apply {
        blocks += code
    }

    fun addMutatorField(cls: KSClassDeclaration, prop: KSPropertyDeclaration) = apply {
        val clsName = getClassName(cls)
        val fieldName = getPropertyName(prop)

        add(
            """
                @MutatorDsl
                inline val Mutator<$clsName>.$fieldName
                    get() = get().$fieldName.mutator()
                        .onChange { $fieldName -> modifyValue { get().copy($fieldName = $fieldName) } }

            """.trimIndent()
        )
    }

    fun addGetterSetterField(cls: KSClassDeclaration, prop: KSPropertyDeclaration) = apply {
        val clsName = getClassName(cls)
        val fieldName = getPropertyName(prop)

        add(
            """
                @MutatorDsl
                inline var Mutator<$clsName>.$fieldName
                    get() = get().$fieldName
                    set(v) = modifyIfChanged(get().$fieldName, v) { it.copy($fieldName = v) }

            """.trimIndent()
        )
    }
}
