package io.peekandpoke.ultra.reflection

import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KProperty1
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.full.createType
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.full.withNullability
import kotlin.reflect.jvm.isAccessible

/**
 * Creates a reified version of the given [KType]
 *
 * What does that mean? What do you get?
 *
 * - a map of [ctorParams2Types] where generic type parameters are fully reified.
 * - a map of [ctorFields2Types] where generic type parameters are fully reified.
 *
 * This is very handy e.g. for serialization and de-serialization.
 *
 * @param type the type to reify; its type arguments supply the concrete types for the type
 *   parameters of [cls].
 */
class ReifiedKType(val type: KType) {

    /**
     * Raw cls of the [type]
     *
     * Throws when the classifier of [type] is not a class, e.g. a type parameter.
     */
    @Suppress("UNCHECKED_CAST")
    val cls = type.classifier as? KClass<Any>
        ?: error("The classifier of [$type] must be a class, but was [${type.classifier}]")

    /**
     * Primary constructor of [cls], or `null` when there is none.
     *
     * Always present for data and value classes.
     */
    val ctor = cls.primaryConstructor

    /**
     * All properties declared in the type and all supertypes, made accessible.
     *
     * See [KClass.memberProperties]
     */
    val allProperties: List<KProperty1<Any, *>> by lazy {
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
    val allPropertiesToTypes: List<Pair<KProperty1<Any, *>, KType>> by lazy {
        allProperties.map { it to reifyType(it.returnType) }
    }

    /**
     * All properties declared directly in the type, made accessible.
     *
     * See [KClass.declaredMemberProperties]
     */
    val declaredProperties: List<KProperty1<Any, *>> by lazy {
        cls.declaredMemberProperties
            .onEach { it.isAccessible = true }
            .toList()
    }

    /**
     * All properties declared directly in the type associated with their reified types.
     *
     * See [reifyType]
     * See [KClass.declaredMemberProperties]
     */
    val declaredPropertiesToTypes: List<Pair<KProperty1<Any, *>, KType>> by lazy {
        declaredProperties.map { it to reifyType(it.returnType) }
    }

    /**
     * Ctor parameters paired with their reified types, empty when [ctor] is `null`.
     *
     * See [reifyType]
     */
    val ctorParams2Types: List<Pair<KParameter, KType>> by lazy {
        ctor?.parameters
            ?.map { it to reifyType(it.type) }
            ?: emptyList()
    }

    /**
     * The [declaredProperties] backing each ctor parameter, paired with their reified types.
     *
     * Assumes every ctor parameter is declared as a property, as data and value classes guarantee.
     */
    val ctorFields2Types: List<Pair<KProperty1<Any, *>, KType>> by lazy {
        ctorParams2Types.map { (param, type) ->
            val property = declaredProperties.firstOrNull { it.name == param.name }
                ?: error(
                    "Ctor parameter '${param.name}' of [$cls] is not declared as a property"
                )

            property to type
        }
    }

    /**
     * Internal helper for reifying generic types of properties of the class
     *
     * Type parameters are looked up positionally in [cls] and substituted from the arguments of
     * [type]. Star projections are passed through unchanged, as there is nothing to substitute.
     */
    private fun reifyType(subject: KType): KType = when (val classifier = subject.classifier) {

        // Do we have a type parameter here?
        is KTypeParameter -> {
            // Which type parameter do we have?
            val index = cls.typeParameters.indexOf(classifier)
            // Get the real type from the rootType. A parameter of an enclosing class is not in
            // this class' list, so the index can be -1 and there is nothing to substitute.
            val resolved = type.arguments.getOrNull(index)?.type ?: TypeRef.Any.type

            // A nullable use-site stays nullable, even when T substitutes to a non-null type.
            // Losing this makes a `T?` field reject null on deserialization.
            when {
                subject.isMarkedNullable -> resolved.withNullability(true)
                else -> resolved
            }
        }

        // Do we have a class?
        is KClass<*> -> {
            // Let's reify all the classes type parameters as well
            classifier.createType(
                subject.arguments.map { arg ->
                    // KTypeProjection requires variance and type to be null together, so a star
                    // projection must be left alone - giving it a type throws.
                    arg.type?.let { arg.copy(type = reifyType(it)) } ?: arg
                },
                subject.isMarkedNullable
            )
        }

        // Otherwise we take the type as is
        else -> subject
    }
}
