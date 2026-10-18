package io.peekandpoke.ultra.slumber.builtin.objects

import io.peekandpoke.ultra.slumber.Awaker
import io.peekandpoke.ultra.slumber.Slumberer

/** Codec for Kotlin `object` singletons. Deserializes to the singleton [instance]; serializes to an empty map. */
class ObjectInstanceCodec(private val instance: Any) : Awaker, Slumberer {

    // TODO(scan): no validation - ANY non-null input yields the singleton, so `"garbage"` or a map for
    //   a completely different type awakes successfully.
    override fun awake(data: Any?, context: Awaker.Context): Any? {
        if (data == null) {
            return null
        }

        return instance
    }

    // TODO(scan): always the empty map, so a @Slumber.Field property on an `object` is dropped - and
    //   BuiltInModule picks this branch before the data-class one, so a `data object` loses them too.
    override fun slumber(data: Any?, context: Slumberer.Context): Any? {

        if (data == null) {
            return null
        }

        return emptyMap<Any, Any>()
    }
}
