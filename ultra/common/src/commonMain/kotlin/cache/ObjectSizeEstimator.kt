package de.peekandpoke.ultra.common.cache

import de.peekandpoke.ultra.common.WeakSet
import de.peekandpoke.ultra.common.cache.ObjectSizeEstimatorImpl.EstimatorConfig

interface ObjectSizeEstimator {
    companion object {
        operator fun invoke(cfg: EstimatorConfig = EstimatorConfig()): ObjectSizeEstimator {
            return ObjectSizeEstimatorImpl(cfg)
        }
    }

    fun estimate(obj: Any?): Long
}

class ObjectSizeEstimatorImpl(
    val cfg: EstimatorConfig = EstimatorConfig(),
) : ObjectSizeEstimator {
    companion object {
        const val NULL_SIZE = 4L

        const val BOOL_SIZE = 1L
        const val BYTE_SIZE = 1L
        const val CHAR_SIZE = 2L
        const val SHORT_SIZE = 2L
        const val INT_SIZE = 4L
        const val LONG_SIZE = 8L
        const val FLOAT_SIZE = 4L
        const val DOUBLE_SIZE = 8L
    }

    private val seen = WeakSet<Any?>()

    data class EstimatorConfig(
        val objectHeader: Long = 16L,
        val arrayHeader: Long = 24L,
        val pointerSize: Long = 8L,
    )

    override fun estimate(obj: Any?): Long {
        // avoid cycles
        if (seen.contains(obj)) return 0L

        seen.add(obj)

        return when (obj) {
            null -> NULL_SIZE

            is Boolean -> BOOL_SIZE
            is Byte -> BYTE_SIZE
            is Char -> CHAR_SIZE
            is Short -> SHORT_SIZE
            is Int -> INT_SIZE
            is Long -> LONG_SIZE
            is Float -> FLOAT_SIZE
            is Double -> DOUBLE_SIZE

            is String -> {
                val chars = obj.length.toLong() * CHAR_SIZE
                // return
                cfg.objectHeader + cfg.pointerSize + cfg.arrayHeader + chars
            }

            is BooleanArray -> cfg.arrayHeader + BOOL_SIZE * obj.size
            is ByteArray -> cfg.arrayHeader + BYTE_SIZE * obj.size
            is ShortArray -> cfg.arrayHeader + SHORT_SIZE * obj.size
            is CharArray -> cfg.arrayHeader + CHAR_SIZE * obj.size
            is IntArray -> cfg.arrayHeader + INT_SIZE * obj.size
            is LongArray -> cfg.arrayHeader + LONG_SIZE * obj.size
            is FloatArray -> cfg.arrayHeader + FLOAT_SIZE * obj.size
            is DoubleArray -> cfg.arrayHeader + DOUBLE_SIZE * obj.size

            is Array<*> -> {
                var sum = cfg.arrayHeader + obj.size.toLong() * cfg.pointerSize
                for (e in obj) sum += estimate(e)
                // return
                sum
            }

            is Collection<*> -> {
                var sum = cfg.objectHeader + obj.size.toLong() * cfg.pointerSize
                for (e in obj) sum += estimate(e)
                // return
                sum
            }

            is Map<*, *> -> {
                var sum = cfg.objectHeader + obj.size.toLong() * 2 * cfg.pointerSize
                for ((k, v) in obj) {
                    sum += estimate(k)
                    sum += estimate(v)
                }
                // return
                sum
            }

            else -> estimateObject(obj)
        }
    }

    private fun estimateObject(obj: Any): Long {

        val fields = ObjectSizeEstimatorPlatform.getFieldsOf(obj)
            ?: return cfg.objectHeader + 2L * cfg.pointerSize // No reflection (e.g., Native): charge a plain object.

        var sum = cfg.objectHeader + fields.size.toLong() * cfg.pointerSize
        for (f in fields) sum += estimate(f)

        return sum
    }
}
