package de.peekandpoke.ultra.meta

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.asTypeName
import de.peekandpoke.ultra.common.startsWithNone
import de.peekandpoke.ultra.meta.model.MType
import de.peekandpoke.ultra.meta.model.Model
import javax.lang.model.element.TypeElement
import javax.lang.model.type.DeclaredType
import javax.lang.model.type.TypeMirror
import javax.lang.model.util.SimpleTypeVisitor7

class GenericUsages(val model: Model) : ProcessorUtils {

    override val ctx = model.ctx

    private val registry = mutableMapOf<ClassName, MutableSet<MType>>()

    fun get(cls: ClassName): Set<MType> = registry[cls] ?: setOf()

    fun add(type: TypeElement) = apply {

        type.variables
            // We only take variables that have a parameterized type
            .filter { it.asTypeName() is ParameterizedTypeName }
            // We extract all the generic information - for nested generic types as well
            .flatMap { variable ->

                val allGenericTypes = mutableSetOf(
                    variable.asType() as DeclaredType
                )

                /**
                 * Recursive function for reading all types
                 */
                fun gather(type: TypeMirror): Unit = type.accept(object : SimpleTypeVisitor7<Unit, Void?>() {

                    override fun visitDeclared(t: DeclaredType, p: Void?) {

                        if (t.typeArguments.isNotEmpty()) {
                            allGenericTypes.add(t)
                        }

                        t.typeArguments.forEach { gather(it) } // recurse through all type arguments
                    }
                }, null)

                // gather all types
                gather(variable.asType())

                // return all gathered generic types
                allGenericTypes
            }
            // Blacklist default type "java.*, "kotlin.*" ...
            .blacklist()
            // Create a reified type from each generic type we have found
            .map { declaredType ->

                val parameterized = declaredType.asTypeName() as ParameterizedTypeName
                val variableType = declaredType.asElement() as TypeElement

                MType(model, variableType, parameterized)
            }
            .forEach { type ->
                registry.getOrPut(type.className) { mutableSetOf() }.add(type)
            }
    }

    /**
     * Black list some packages
     */
    private fun List<DeclaredType>.blacklist() = asSequence()
        .distinct()
        .filter { it.fqn.startsWithNone(defaultPackageBlackList) }
        .toList()
}
