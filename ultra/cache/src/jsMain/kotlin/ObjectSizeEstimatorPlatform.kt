package io.peekandpoke.ultra.cache

/** JS implementation that extracts fields via `Object.getOwnPropertyNames`. */
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual object ObjectSizeEstimatorPlatform {
    /**
     * Reads the own properties of [obj], skipping function-valued ones. Never returns `null`.
     *
     * Prototype members are not own properties, so inherited state is not seen.
     */
    // TODO(scan): reading `dyn[k]` invokes an own accessor property, so estimating can run user code
    //  and can throw - the JVM actual reads the raw field instead.
    actual fun getFieldsOf(obj: Any): List<Any?>? {
        val dyn = obj.asDynamic()
        val keys = js("Object.getOwnPropertyNames")(dyn) as Array<String>

        val out = ArrayList<Any?>(keys.size)

        for (k in keys) {
            val v = dyn[k]
            // jsTypeOf, not js("typeof v === 'function'"): the inlined form reaches for the Kotlin
            // local `v` by name, and a renamed local would make it `typeof <undeclared>`, which
            // yields "undefined" rather than throwing - i.e. the check would silently invert.
            if (jsTypeOf(v) != "function") out.add(v)
        }

        return out.toList()
    }
}
