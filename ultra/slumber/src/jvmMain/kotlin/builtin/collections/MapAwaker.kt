package io.peekandpoke.ultra.slumber.builtin.collections

import io.peekandpoke.ultra.reflection.TypeRef
import io.peekandpoke.ultra.slumber.Awaker
import kotlin.reflect.KType
import kotlin.reflect.full.withNullability

/**
 * Deserializes raw Maps into typed Kotlin Maps.
 *
 * Both keys and values are recursively awakened using the map's type arguments.
 */
class MapAwaker(
    private val keyType: KType,
    private val valueType: KType,
    private val creator: List<Pair<*, *>>.() -> Map<*, *>,
) : Awaker {

    companion object {
        /**
         * Creates a [MapAwaker] for the given map [type], reading key and value types off its type
         * arguments. A star-projected argument has no type, so a fallback is used.
         */
        fun forMap(type: KType): MapAwaker {
            // TODO(scan): the fallback forces the keys of a star-projected map through StringAwaker,
            //  so `Map<*, V>` silently retypes an Int key 2 to the String "2".
            val keyType = type.arguments.getOrNull(0)?.type ?: TypeRef.String.type

            // TODO(scan): this guard never fires for the case it was written for (a69b2f65, "allowing
            //  null values ... in nested generic maps"): the elvis already replaced a star projection
            //  with TypeRef.Any, whose classifier is non-null, so `Map<K, *>` keeps a NON-nullable
            //  value type and a null value throws. `?: TypeRef.AnyNull.type` is what was meant.
            val valueType = (type.arguments.getOrNull(1)?.type ?: TypeRef.Any.type).let {
                if (it.classifier != null) {
                    it
                } else {
                    it.withNullability(true)
                }
            }

            return MapAwaker(keyType, valueType) {
                toMap().toMutableMap()
            }
        }
    }

    /**
     * Awakes keys and values through their own types. Input that is not a `Map` awakes to `null`,
     * which a non-nullable declared type then turns into an error via `NonNullAwaker`.
     */
    override fun awake(data: Any?, context: Awaker.Context): Any? {

        if (data !is Map<*, *>) {
            return null
        }

        // TODO(scan): keys are awoken BEFORE the map is rebuilt, so two raw keys that awake to the
        //  same key collapse silently — last one wins, no error. E.g. Map<Int, V> from {"1": a, 1: b}.
        return data
            .map { (k, v) ->
                // TODO(scan): the RAW key goes into the path unbounded and unescaped. AwakerException
                //  truncates the input value to 8192 chars but not the path, so a huge attacker-chosen
                //  key is echoed in full into the message and into every tracking log line.
                context.stepInto("$k[KEY]").awake(keyType, k) to
                        context.stepInto("$k[VAL]").awake(valueType, v)
            }
            .creator()
    }
}
