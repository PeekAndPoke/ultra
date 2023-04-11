package de.peekandpoke.ultra.slumber.builtin.objects

import de.peekandpoke.ultra.common.reflection.ReifiedKType
import de.peekandpoke.ultra.common.reflection.hasAnyAnnotationRecursive
import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.AwakerException
import de.peekandpoke.ultra.slumber.Slumber
import de.peekandpoke.ultra.slumber.Slumberer
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.withNullability
import kotlin.reflect.jvm.isAccessible

class DataClassCodec(rootType: KType) : Awaker, Slumberer {

    /** Raw cls of the rootType */
    private val reified = ReifiedKType(rootType)

    private val primaryCtor = reified.ctor

    private val nullables: Map<KParameter, Any?> = primaryCtor?.parameters
        ?.filter { it.type.isMarkedNullable }
        ?.associate { it to null }
        ?: emptyMap()

    private val allSlumberFields = reified.ctorFields2Types
        .plus(
            reified.allPropertiesToTypes
                .filter { (prop, _) -> prop.hasAnyAnnotationRecursive { it is Slumber.Field } }
        )
        .distinctBy { (prop, _) -> prop }

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
                    // For optional parameter we fall back to the default value or the parameter
                    // even when the inner object has a deserialization problem.
                    else -> try {
                        context.stepInto(paramName).awake(type.withNullability(true), raw)
                    } catch (ex: AwakerException) {
                        null
                    }
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
            else -> {
                context.log {
                    "${reified.type} misses parameters ${missingParams.joinToString { "'$it'" }}"
                }

                null
            }
        }
    }

    override fun slumber(data: Any?, context: Slumberer.Context): Map<String, Any?>? = when {

        data != null -> {
            allSlumberFields.associate { (prop, _) ->
                prop.name to context.slumber(prop.get(data))
            }
        }

        else -> null
    }
}
