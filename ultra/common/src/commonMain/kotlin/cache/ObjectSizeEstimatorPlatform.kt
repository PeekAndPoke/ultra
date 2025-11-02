package de.peekandpoke.ultra.common.cache

@Suppress("EXPECT_ACTUAL_CLASSIFIERS_ARE_IN_BETA_WARNING", "unused")
expect object ObjectSizeEstimatorPlatform {
    fun getFieldsOf(obj: Any): List<Any?>?
}
