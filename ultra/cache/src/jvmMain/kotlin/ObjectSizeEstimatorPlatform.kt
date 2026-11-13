package io.peekandpoke.ultra.cache

import java.lang.reflect.Field
import java.lang.reflect.Modifier

/** JVM implementation that extracts fields via `java.lang.reflect`. */
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual object ObjectSizeEstimatorPlatform {

    /** The readable fields per class, resolved once. A [ClassValue] lives on the class, so it pins no class loader. */
    private val fieldsByClass = object : ClassValue<List<Field>>() {
        override fun computeValue(type: Class<*>): List<Field> = readableFieldsOf(type)
    }

    /**
     * Reads the instance fields of [obj], walking the class hierarchy up to (excluding) [Any].
     *
     * Skips static and synthetic fields (an inner class's outer instance, an anonymous object's
     * captured locals; a lambda's captures are ordinary fields and are read). A field JPMS does not
     * open is skipped, too. Never returns `null`.
     */
    actual fun getFieldsOf(obj: Any): List<Any?>? = fieldsByClass.get(obj.javaClass).map { it.get(obj) }

    private fun readableFieldsOf(type: Class<*>): List<Field> {
        val out = ArrayList<Field>()
        var cls: Class<*>? = type

        while (cls != null && cls !== Any::class.java) {

            for (f in cls.declaredFields) {
                if (Modifier.isStatic(f.modifiers) || f.isSynthetic) continue

                // Reports a JPMS refusal instead of throwing it, so nothing needs catching here or in
                // getFieldsOf - where a blanket catch used to hide real errors as well.
                if (f.trySetAccessible()) {
                    out.add(f)
                }
            }

            cls = cls.superclass
        }

        return out
    }
}
