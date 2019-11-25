package de.peekandpoke.ultra.meta

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.asTypeName
import de.peekandpoke.ultra.meta.model.MType
import de.peekandpoke.ultra.meta.model.Model
import javax.lang.model.element.TypeElement
import javax.lang.model.element.VariableElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeMirror
import javax.lang.model.type.TypeVariable

class GenericUsages(val model: Model, types: List<TypeElement>) : ProcessorUtils {

    override val ctx = model.ctx

    private val registry = mutableMapOf<ClassName, MutableSet<MType>>()

    private val visitedDeclaredTypes = mutableSetOf<DeclaredType>()

    init {
        types.forEach { visit(it) }
    }

    fun get(cls: ClassName): Set<MType> = registry[cls] ?: setOf()

    private fun DeclaredType.isReified() = typeArguments.all { it is DeclaredType }

    private fun put(declaredType: DeclaredType) = put(
        MType(
            model = model,
            type = declaredType.asElement() as TypeElement,
            typeName = declaredType.asTypeName() as ParameterizedTypeName
        )
    )

    private fun put(mType: MType) {
        registry.getOrPut(mType.className) { mutableSetOf() }.add(mType)
    }

    private fun visit(type: TypeElement) {
        type.variables.forEach { visit(it) }
    }

    private fun visit(variable: VariableElement) {
        visit(variable.asType())
    }

    private fun visit(typeMirror: TypeMirror) {
        if (typeMirror is DeclaredType) {
            visit(typeMirror)
        }
    }

    private fun visit(declaredType: DeclaredType) {

        // avoid endless loops and blacklisted types
        if (visitedDeclaredTypes.contains(declaredType) ||
            declaredType.typeArguments.isEmpty() ||
            !declaredType.isReified()
        ) {
            return
        }

        // put this type into the visited types
        visitedDeclaredTypes.add(declaredType)

        // put this type into the registry
        put(declaredType)

        // visit all type arguments of the type
        declaredType.typeArguments.forEach { visit(it) }

        // visit the type
        val type = declaredType.asElement() as TypeElement

        // visit all variable that have reified type parameters already
        type.variables
            .filter { v -> v.asType().let { it is DeclaredType && it.isReified() } }
            .forEach { visit(it) }

        // We have to reify type parameters for some variables
        type.variables
            .map { it.asType() }
            .filterIsInstance<DeclaredType>()
            .filter { !it.isReified() }
            .forEach {
                visit(
                    it.reify(declaredType)
                )
            }
    }

    /**
     * Reifies the type variables of the receiver with type information from the [provider]
     */
    private fun DeclaredType.reify(provider: DeclaredType): TypeMirror {

        // visit the type
        val providerElement = provider.asElement() as TypeElement

        // Create a new declared type by mapping type variables to real types
        return typeUtils.getDeclaredType(
            // the variable element as a type element
            asElement() as TypeElement,
            // we try to map all type arguments to real types
            *typeArguments.map { typeArg ->
                when (typeArg) {
                    is TypeVariable -> {
                        // Get the index of the type arg
                        val idx = providerElement.typeParameters.indexOfFirst {
                            it == typeArg.asElement()
                        }
                        // Get the real type from the provider by the idx we found
                        when (idx) {
                            -1 -> typeArg
                            else -> provider.typeArguments[idx]
                        }
                    }
                    else -> typeArg
                }
            }.toTypedArray()
        )
    }
}
