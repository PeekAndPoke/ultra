package io.peekandpoke.ultra.cache

/** Native implementation — returns `null` because Kotlin/Native lacks runtime reflection. */
@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING")
actual object ObjectSizeEstimatorPlatform {
    actual fun getFieldsOf(obj: Any): List<Any?>? {
        // Native does not provide runtime reflection for walking object fields.
        return null
    }
}
