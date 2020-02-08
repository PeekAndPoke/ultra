package de.peekandpoke.ultra.common

import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.full.createType
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.javaField

class ReifiedKType(val type: KType) {

    /**
     * Raw cls of the [type]
     */
    val cls = type.classifier as KClass<*>

    /**
     * The ctor is always present for a data class
     */
    val ctor = cls.primaryConstructor!!

    /**
     * Map of ctor parameters to their reified types
     */
    val ctorParams2Types = ctor.parameters.map { it to reifyType(it.type) }

    /**
     * Map of properties corresponding to the ctor param to their reified types
     */
    val properties2Types = ctorParams2Types.map { (param, type) ->
        cls.declaredMemberProperties
            .mapNotNull { it.javaField }
            .first { it.name == param.name }
            .apply { isAccessible = true } to type
    }

    /**
     * Internal helper for reifying generic types of properties of the class
     */
    private fun reifyType(subject: KType): KType = when (val classifier = subject.classifier) {

        // Do we have a type parameter here?
        is KTypeParameter -> {
            // Which type parameter do we have?
            val index = cls.typeParameters.indexOf(classifier)
            // Get the real type from the rootType
            type.arguments[index].type ?: Any::class.createType()
        }

        // Do we have a class?
        is KClass<*> -> {
            // Let's reify all of the classes type parameters as well
            classifier.createType(
                subject.arguments.map {
                    it.copy(type = reifyType(it.type ?: Any::class.createType()))
                },
                subject.isMarkedNullable
            )
        }

        // Otherwise we take the type as is
        else -> subject
    }
}
