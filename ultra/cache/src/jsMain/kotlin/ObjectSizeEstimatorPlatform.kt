package io.peekandpoke.ultra.cache

/** JS implementation that extracts fields via `Object.getOwnPropertyNames`. */
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual object ObjectSizeEstimatorPlatform {
    actual fun getFieldsOf(obj: Any): List<Any?>? {
        val dyn = obj.asDynamic()
        val keys = js("Object.getOwnPropertyNames")(dyn) as Array<String>

        val out = ArrayList<Any?>(keys.size)

        for (k in keys) {
            val v = dyn[k]
            val isFn = js("typeof v === 'function'") as Boolean
            if (!isFn) out.add(v)
        }

        return out.toList()
    }
}
