package io.peekandpoke.ultra.slumber.builtin.objects

import io.peekandpoke.ultra.reflection.ReifiedKType
import io.peekandpoke.ultra.slumber.Awaker
import io.peekandpoke.ultra.slumber.AwakerException
import io.peekandpoke.ultra.slumber.NonNullAwaker
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.withNullability
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaConstructor
import kotlin.reflect.jvm.javaMethod

/**
 * Deserializes a `Map` into a data class instance by matching map keys to primary-constructor
 * parameter names.
 *
 * Anything that is not a `Map` awakes to `null` — including `null` itself — which a surrounding
 * [NonNullAwaker] turns into an [AwakerException] for a non-nullable declared type.
 * Keys with no matching parameter are ignored. A parameter whose key is missing and
 * which is neither nullable nor optional makes the whole object awake to `null`, with the offending
 * names written to [Awaker.Context.log].
 */
interface DataClassAwaker : Awaker {

    companion object {
        /** Creates a [DataClassAwaker] for the given data class [type]. */
        operator fun invoke(type: KType): DataClassAwaker = Default(type)
    }

    private class Default(type: KType) : DataClassAwaker {
        /** Raw cls of the rootType */
        val reified = ReifiedKType(type)

        /** Gets the primary Ctor */
        val primaryCtor = reified.ctor

        // TODO(scan): redundant - every entry here is written again by the awake() loop, which
        //   reaches the same nullable parameters through reified.ctorParams2Types.
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

                // TODO(scan): the lookup key and the read key differ for an unnamed parameter -
                //   contains() probes null while the get() below probes the literal "n/a".
                val paramName = param.name ?: "n/a"

                // Do we have data for param ?
                if (data.contains(param.name)) {

                    val raw = data[paramName]

                    // Get the value and awake it
                    val bit = when (param.isOptional) {
                        false -> context.stepInto(paramName).awake(type, raw)
                        // For optional parameter we fall back to the default value or the parameter
                        // even when the inner object has a deserialization problem.
                        // TODO(scan): withNullability(true) allocates a fresh KType per field per call,
                        //   and every one of them is memoized in SlumberConfig.Lookup.
                        else -> try {
                            context.stepInto(paramName).awake(type.withNullability(true), raw)
                        } catch (_: AwakerException) {
                            null
                        }
                    }

                    // TODO(scan): nullable is tested before optional, so a nullable parameter that also
                    //   has a default never falls back to it - `val a: String? = "x"` awakes to null.
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
                    // TODO(scan): same ordering problem as above, for the absent-key case.
                    when {
                        param.type.isMarkedNullable -> params[param] = null

                        param.isOptional -> {
                            // do nothing
                        }

                        else -> missingParams.add(paramName)
                    }
                }
            }

            // callBy, not call: parameters left out of the map are exactly the optional ones, and
            // callBy is what applies their defaults.
            return when {
                // When we have all the parameters we need, we can call the ctor.
                missingParams.isEmpty() && primaryCtor != null -> primaryCtor.callBy(params)
                // Otherwise, we have to return null
                // TODO(scan): a null primaryCtor lands here too and logs "misses parameters" with an
                //   empty list, which describes the wrong problem.
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
