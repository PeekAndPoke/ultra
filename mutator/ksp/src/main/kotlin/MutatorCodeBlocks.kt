package de.peekandpoke.mutator.ksp

import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import de.peekandpoke.mutator.ListMutator
import de.peekandpoke.mutator.ObjectMutator
import de.peekandpoke.mutator.SetMutator
import de.peekandpoke.ultra.common.surround

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

    fun wrapWithBackticks(str: String) = str.surround("`")

    fun getClassName(cls: KSClassDeclaration): String {
        return cls.getSimpleNames().joinToString(".") { wrapWithBackticks(it.asString()) }
    }

    fun getTypeParams(cls: KSClassDeclaration): String? {
        if (cls.typeParameters.isEmpty()) return null

        return cls.typeParameters
            .joinToString(", ") { it.name.asString() }
            .surround("<", ">")
    }

    fun getTypeParamsWithBounds(cls: KSClassDeclaration): String? {
        if (cls.typeParameters.isEmpty()) return null

        return cls.typeParameters.joinToString(", ") {
            val name = it.name.asString()

            val bounds = it.bounds
                .map { bound -> bound.resolve() }
                // We ignore all classes that do not have a qualified name
                .filterNot { bound ->
                    bound.declaration.qualifiedName == null
                }
                // We ignore kotlin.Any? as this is the default bound
                .filterNot { bound ->
                    bound.declaration.qualifiedName?.asString() == Any::class.qualifiedName
                            && bound.isMarkedNullable
                }
                .joinToString(", ") { bound ->
                    getTypeString(bound.declaration, bound.isMarkedNullable)
                }

            if (bounds.isEmpty()) name else "$name : $bounds"
        }.surround("<", ">")
    }

    fun getClassNameWithTypeParams(cls: KSClassDeclaration): String {
        return "${getClassName(cls)}${getTypeParams(cls) ?: ""}"
    }

    fun getPropertyName(prop: KSPropertyDeclaration): String {
        return wrapWithBackticks(prop.simpleName.asString())
    }

    fun getTypeString(declaration: KSDeclaration, nullable: Boolean): String {
        val nullableStr = if (nullable) "?" else ""

        val cls = when (declaration.isPrimitiveOrString()) {
            true -> declaration.simpleName.asString()
            else -> declaration.qualifiedName!!.asString()
        }

        return wrapWithBackticks(cls) + nullableStr
    }

    fun prepend(code: String) = apply {
        blocks.add(0, code)
    }

    fun append(code: String) = apply {
        blocks += code
    }

    fun addObjectMutatorField(cls: KSClassDeclaration, prop: KSPropertyDeclaration) = apply {
        val clsName = getClassNameWithTypeParams(cls)
        val typeParams = getTypeParamsWithBounds(cls)?.plus(" ") ?: ""
        val fieldName = getPropertyName(prop)

        append(
            """
                @MutatorDsl
                inline val ${typeParams}Mutator<$clsName>.$fieldName
                    get() = get().$fieldName.mutator()
                        .onChange { $fieldName -> modifyValue { get().copy($fieldName = $fieldName) } }

            """.trimIndent()
        )
    }

    fun addCollectionMutatorField(cls: KSClassDeclaration, prop: KSPropertyDeclaration) = apply {
        val clsName = getClassNameWithTypeParams(cls)
        val typeParams = getTypeParamsWithBounds(cls)?.plus(" ") ?: ""
        val fieldName = getPropertyName(prop)

        append(
            """
                @MutatorDsl
                inline val ${typeParams}Mutator<$clsName>.$fieldName
                    get() = get().$fieldName.mutator()
                        .onChange { $fieldName -> modifyValue { get().copy($fieldName = $fieldName) } }

            """.trimIndent()
        )
    }

    fun addObjectPureField(cls: KSClassDeclaration, prop: KSPropertyDeclaration) = apply {
        val clsName = getClassNameWithTypeParams(cls)
        val typeParams = getTypeParamsWithBounds(cls)?.plus(" ") ?: ""
        val fieldName = getPropertyName(prop)

        append(
            """
                @MutatorDsl
                inline var ${typeParams}Mutator<$clsName>.$fieldName
                    get() = get().$fieldName
                    set(v) = modifyIfChanged(get().$fieldName, v) { it.copy($fieldName = v) }

            """.trimIndent()
        )
    }
}
