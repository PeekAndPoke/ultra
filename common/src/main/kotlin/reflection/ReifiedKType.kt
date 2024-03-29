package de.peekandpoke.ultra.common.reflection

import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.full.createType
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible

/**
 * Creates a reified version of the given [KType]
 *
 * What does that mean? What to you get?
 *
 * - a map of [ctorParams2Types] where generic type parameters are fully reified.
 * - a map of [ctorFields2Types] where generic type parameters are fully reified.
 *
 * This is very handy e.g. for serialization and de-serialization.
 */
class ReifiedKType(val type: KType) {

    /**
     * Raw cls of the [type]
     */
    @Suppress("UNCHECKED_CAST")
    val cls = type.classifier as KClass<Any>

    /**
     * The ctor is always present for a data class
     */
    val ctor = cls.primaryConstructor

    /**
     * All properties declared in the type and all supertypes
     *
     * See [KClass.memberProperties]
     */
    // TODO: test me
    val allProperties: List<KProperty1<Any, *>> by lazy(LazyThreadSafetyMode.NONE) {
        cls.memberProperties
            .onEach { it.isAccessible = true }
            .toList()
    }

    /**
     * All properties declared in the type and all supertypes associated with their reified types.
     *
     * See [reifyType]
     * See [KClass.memberProperties]
     */
    // TODO: test me
    val allPropertiesToTypes: List<Pair<KProperty1<Any, *>, KType>> by lazy(LazyThreadSafetyMode.NONE) {
        allProperties.map { it to reifyType(it.returnType) }
    }

    /**
     * All properties declared directly in the type (see [KClass.declaredMemberProperties]
     */
    // TODO: test me
    val declaredProperties: List<KProperty1<Any, *>> by lazy(LazyThreadSafetyMode.NONE) {
        cls.declaredMemberProperties
            .onEach { it.isAccessible = true }
            .toList()
    }

    /**
     * All properties declared directly in the type associated with their reified types.
     *
     * See [reifyType]
     * See [KClass.memberProperties]
     */
    // TODO: test me
    val declaredPropertiesToTypes: List<Pair<KProperty1<Any, *>, KType>> by lazy(LazyThreadSafetyMode.NONE) {
        declaredProperties.map { it to reifyType(it.returnType) }
    }

    /**
     * Map of ctor parameters to their reified types
     *
     * See [reifyType]
     */
    val ctorParams2Types: List<Pair<KParameter, KType>> by lazy(LazyThreadSafetyMode.NONE) {
        ctor?.parameters
            ?.map { it to reifyType(it.type) }
            ?: emptyList()
    }

    /**
     * Map of properties corresponding to the ctor param to their reified types
     */
    val ctorFields2Types: List<Pair<KProperty1<Any, *>, KType>> by lazy(LazyThreadSafetyMode.NONE) {
        ctorParams2Types.map { (param, type) ->
            declaredProperties
                .first { it.name == param.name }
                .let { it to type }
        }
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
            type.arguments[index].type ?: TypeRef.Any.type
        }

        // Do we have a class?
        is KClass<*> -> {
            // Let's reify all the classes type parameters as well
            classifier.createType(
                subject.arguments.map {
                    it.copy(type = reifyType(it.type ?: TypeRef.Any.type))
                },
                subject.isMarkedNullable
            )
        }

        // Otherwise we take the type as is
        else -> subject
    }
}
