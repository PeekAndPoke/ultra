package de.peekandpoke.ultra.slumber.builtin.objects

import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.Shared
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.primaryConstructor

class DataClassAwaker(cls: KClass<*>, shared: Shared) : Awaker {

    private val fqn = cls.qualifiedName

    private val ctor = cls.primaryConstructor!!

    private val ctorParams2Awakers =
        ctor.parameters.map { it to shared.getAwaker(it.type) }

    private val nullables: Map<KParameter, Any?> =
        ctor.parameters.filter { it.type.isMarkedNullable }.map { it to null }.toMap()

    override fun awake(data: Any?): Any? {

        // Do we have some data that we can work with?
        if (data !is Map<*, *>) {
            return null
        }

        // We start with all the nullable parameters
        val params = nullables.toMutableMap()
        // We track all the missing parameters for better error reporting
        val missingParams = mutableListOf<String>()

        // We go through all the parameters of the primary ctor
        ctorParams2Awakers.forEach { (param, awaker) ->

            // Do we have data for param ?
            if (data.contains(param.name)) {
                // Get the value and awake it
                val bit = awaker.awake(data[param.name])

                // can we use the bit ?
                if (bit != null || param.type.isMarkedNullable) {
                    params[param] = awaker.awake(data[param.name])
                } else {
                    missingParams.add(param.name ?: "n/a")
                }

                // Yes so let's awake it and put it into the params
            } else {
                // Last chance: is the parameter optional or nullable ?
                if (!param.isOptional && !param.type.isMarkedNullable) {
                    // No, so we have problem and cannot awake the data class
                    missingParams.add(param.name ?: "n/a")
                }
            }
        }

        // When we have all the parameters we need, we can call the ctor?
        if (missingParams.isEmpty()) {
            return ctor.callBy(params)
        }

        return null
    }
}
