package de.peekandpoke.ultra.slumber.builtin.objects

import de.peekandpoke.ultra.common.reflection.ReifiedKType
import de.peekandpoke.ultra.slumber.Awaker
import de.peekandpoke.ultra.slumber.AwakerException
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.withNullability
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaConstructor
import kotlin.reflect.jvm.javaMethod

interface DataClassAwaker : Awaker {

    companion object {
        operator fun invoke(type: KType): DataClassAwaker = Default(type)
    }

    private class Default(type: KType) : DataClassAwaker {
        /** Raw cls of the rootType */
        val reified = ReifiedKType(type)

        /** Gets the primary Ctor */
        val primaryCtor = reified.ctor

        /** Nullable fields */
        val nullables: Map<KParameter, Any?> = primaryCtor?.parameters
            ?.filter { it.type.isMarkedNullable }
            ?.associate { it to null }
            ?: emptyMap()

        init {
            // We need to make all constructors accessible.
            // This is necessary so that overloaded constructors with default values can be called correctly.
            // We need to also make the java underlying java methods accessible, as the Kotlin impl is sometimes buggy.
            primaryCtor?.isAccessible = true
            primaryCtor?.javaMethod?.isAccessible = true
            primaryCtor?.javaConstructor?.isAccessible = true

            reified.cls.constructors.forEach {
                it.isAccessible = true
                it.javaMethod?.isAccessible = true
                it.javaConstructor?.isAccessible = true
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
                        } catch (_: AwakerException) {
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
    }

}
