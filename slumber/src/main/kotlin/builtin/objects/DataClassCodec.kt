package de.peekandpoke.ultra.slumber.builtin.objects

import de.peekandpoke.ultra.common.reflection.ReifiedKType
import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.Slumberer
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.withNullability
import kotlin.reflect.jvm.isAccessible

class DataClassCodec(rootType: KType) : Awaker, Slumberer {

    /** Raw cls of the rootType */
    private val reified = ReifiedKType(rootType)

    private val primaryCtor = reified.ctor

    private val nullables: Map<KParameter, Any?> =
        primaryCtor?.parameters?.filter { it.type.isMarkedNullable }?.associate { it to null }
            ?: emptyMap()

    init {
        // We need to make all constructors accessible.
        // This is necessary so that overloaded constructors with default values can be called correctly.
        reified.cls.constructors.forEach {
            it.isAccessible = true
        }
    }

    @Suppress("Detekt:ComplexMethod")
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

                val raw = data[paramName]

                // Get the value and awake it
                val bit = when (param.isOptional) {
                    false -> context.stepInto(paramName).awake(type, raw)
                    else -> context.stepInto(paramName).awake(type.withNullability(true), raw)
                }

                when {
                    bit != null -> params[param] = bit

                    param.type.isMarkedNullable -> params[param] = null

                    param.isOptional -> { /* do nothing */
                    }

                    else -> missingParams.add(paramName)
                }
            }
            // no there is no data for the parameter
            else {
                when {
                    param.type.isMarkedNullable -> params[param] = null

                    param.isOptional -> {
                        // do nothing
                    }

                    else -> missingParams.add(paramName)
                }
            }
        }

        return when {
            // When we have all the parameters we need, we can call the ctor.
            missingParams.isEmpty() && primaryCtor != null -> primaryCtor.callBy(params)
            // Otherwise, we have to return null
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
