package de.peekandpoke.ultra.slumber.builtin.objects

import de.peekandpoke.ultra.common.reflection.ReifiedKType
import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.Slumberer
import kotlin.reflect.KParameter
import kotlin.reflect.KType

class DataClassCodec(rootType: KType) : Awaker, Slumberer {

    /** Raw cls of the rootType */
    private val reified = ReifiedKType(rootType)

    private val ctor = reified.ctor

    private val nullables: Map<KParameter, Any?> =
        ctor?.parameters?.filter { it.type.isMarkedNullable }?.map { it to null }?.toMap()
            ?: emptyMap()

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
        reified.ctorParams2Types.forEach { (param, type) ->

            val paramName = param.name ?: "n/a"

            // Do we have data for param ?
            if (data.contains(param.name)) {
                // Get the value and awake it
                val bit = context.stepInto(paramName).awake(type, data[paramName])

                // can we use the bit ?
                if (bit != null || param.type.isMarkedNullable) {
                    // Yes, we awake it and put it into the params
                    params[param] = bit
                } else {
                    // No, we mark it as a missing parameter
                    missingParams.add(paramName)
                }

            } else {
                // Last chance: is the parameter optional or nullable ?
                if (!param.isOptional && !param.type.isMarkedNullable) {
                    // No, we have problem and cannot awake the data class
                    missingParams.add(paramName)
                }
            }
        }

        return when {
            // When we have all the parameters we need, we can call the ctor.
            missingParams.isEmpty() && ctor != null -> ctor.callBy(params)
            // Otherwise we have to return null
            else -> null
        }
    }

    override fun slumber(data: Any?, context: Slumberer.Context): Map<String, Any?>? = when {

        data != null -> reified.properties2Types.map { (prop, type) ->
            prop.name to context.slumber(type, prop.get(data))
        }.toMap()

        else -> null
    }
}
