package de.peekandpoke.mutator.ksp

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import de.peekandpoke.mutator.ListMutator
import de.peekandpoke.mutator.ObjectMutator
import de.peekandpoke.mutator.SetMutator

class MutatorCodeBlocks {

    companion object {
        val ObjectMutatorName = ObjectMutator::class.simpleName
        val ListMutatorName = ListMutator::class.simpleName
        val SetMutatorName = SetMutator::class.simpleName
    }

    private val blocks = mutableListOf<String>()

    fun isEmpty() = blocks.isEmpty()

    fun isNotEmpty() = !isEmpty()

    fun build() = blocks.joinToString("\n")

    fun wrapWithBackticks(str: String) = "`$str`"

    fun getClassName(cls: KSClassDeclaration): String {
        return cls.getSimpleNames().joinToString(".") { wrapWithBackticks(it.asString()) }
    }

    fun getPropertyName(prop: KSPropertyDeclaration): String {
        return wrapWithBackticks(prop.simpleName.asString())
    }

    fun prepend(code: String) = apply {
        blocks.add(0, code)
    }

    fun append(code: String) = apply {
        blocks += code
    }

    fun addObjectMutatorField(cls: KSClassDeclaration, prop: KSPropertyDeclaration) = apply {
        val clsName = getClassName(cls)
        val fieldName = getPropertyName(prop)

        append(
            """
                @MutatorDsl
                inline val Mutator<$clsName>.$fieldName
                    get() = get().$fieldName.mutator()
                        .onChange { $fieldName -> modifyValue { get().copy($fieldName = $fieldName) } }

            """.trimIndent()
        )
    }

    fun addObjectPureField(cls: KSClassDeclaration, prop: KSPropertyDeclaration) = apply {
        val clsName = getClassName(cls)
        val fieldName = getPropertyName(prop)

        append(
            """
                @MutatorDsl
                inline var Mutator<$clsName>.$fieldName
                    get() = get().$fieldName
                    set(v) = modifyIfChanged(get().$fieldName, v) { it.copy($fieldName = v) }

            """.trimIndent()
        )
    }
}
