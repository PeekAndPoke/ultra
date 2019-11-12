package de.peekandpoke.ultra.slumber.builtin.objects

import de.peekandpoke.ultra.slumber.Awaker
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.full.createType
import kotlin.reflect.full.primaryConstructor

class DataClassAwaker(private val rootType: KType) : Awaker {

    /** Raw cls of the rootType */
    private val cls = rootType.classifier as KClass<*>

    /** The ctor is always present for a data class */
    private val ctor = cls.primaryConstructor!!

    /** Map of ctor parameters to their reified types */
    private val ctorParams2Types = ctor.parameters.map { it to reifyType(it.type) }

    private fun reifyType(type: KType): KType = when (val classifier = type.classifier) {

        // Do we have a type parameter here?
        is KTypeParameter -> {
            // Which type parameter do we have?
            val index = cls.typeParameters.indexOf(classifier)
            // Get the real type from the rootType
            rootType.arguments[index].type ?: Any::class.createType()
        }

        // Do we have a class?
        is KClass<*> -> {
            // Let's reify all of the classes type parameters as well
            classifier.createType(
                type.arguments.map {
                    it.copy(type = reifyType(it.type ?: Any::class.createType()))
                },
                type.isMarkedNullable
            )
        }

        // Otherwise we take the type as is
        else -> type
    }

    private val nullables: Map<KParameter, Any?> =
        ctor.parameters.filter { it.type.isMarkedNullable }.map { it to null }.toMap()

    override fun awake(data: Any?, context: Awaker.Context): Any? {

        // Do we have some data that we can work with?
        if (data !is Map<*, *>) {
            return null
        }

        // We start with all the nullable parameters
        val params = nullables.toMutableMap()
        // We track all the missing parameters for better error reporting
        val missingParams = mutableListOf<String>()

        // We go through all the parameters of the primary ctor
        ctorParams2Types.forEach { (param, type) ->

            val paramName = param.name ?: "n/a"

            // Do we have data for param ?
            if (data.contains(param.name)) {
                // Get the value and awake it
                val bit = context.stepInto(paramName).awake(type, data[paramName])

                // can we use the bit ?
                if (bit != null || param.type.isMarkedNullable) {
                    params[param] = bit
                } else {
                    missingParams.add(paramName)
                }

                // Yes so let's awake it and put it into the params
            } else {
                // Last chance: is the parameter optional or nullable ?
                if (!param.isOptional && !param.type.isMarkedNullable) {
                    // No, so we have problem and cannot awake the data class
                    missingParams.add(paramName)
                }
            }
        }

        return when {
            // When we have all the parameters we need, we can call the ctor.
            missingParams.isEmpty() -> ctor.callBy(params)
            // Otherwise we have to return null
            else -> null
        }
    }
}
