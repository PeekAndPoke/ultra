package io.peekandpoke.ultra.cache

import java.lang.reflect.Modifier

/** JVM implementation that extracts fields via `java.lang.reflect`. */
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual object ObjectSizeEstimatorPlatform {
    actual fun getFieldsOf(obj: Any): List<Any?>? {
        val out = ArrayList<Any?>()
        var cls: Class<*>? = obj.javaClass

        while (cls != null && cls !== Any::class.java) {

            for (f in cls.declaredFields) {
                if (Modifier.isStatic(f.modifiers)) continue

                try {
                    f.isAccessible = true
                    out.add(f.get(obj))
                } catch (_: Throwable) { /* ignore */
                }
            }

            cls = cls.superclass
        }

        return out
    }
}
