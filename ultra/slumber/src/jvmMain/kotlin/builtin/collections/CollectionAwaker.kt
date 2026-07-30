package io.peekandpoke.ultra.slumber.builtin.collections

import io.peekandpoke.ultra.slumber.Awaker
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.typeOf
import java.lang.reflect.Array as JavaArray

/**
 * Deserializes arrays and iterables into typed List, Set or array values.
 *
 * Each element is recursively awakened using the collection's type argument.
 */
class CollectionAwaker(
    private val innerType: KType,
    private val creator: List<*>.() -> Any,
) : Awaker {

    companion object {

        /**
         * Element types of Kotlin's primitive array classes.
         *
         * These carry NO type argument (`IntArray` is not `Array<Int>`), so the element type cannot be
         * read off the [KType] and has to be looked up.
         */
        private val primitiveArrayElements: Map<KClass<*>, KType> = mapOf(
            BooleanArray::class to typeOf<Boolean>(),
            ByteArray::class to typeOf<Byte>(),
            CharArray::class to typeOf<Char>(),
            DoubleArray::class to typeOf<Double>(),
            FloatArray::class to typeOf<Float>(),
            IntArray::class to typeOf<Int>(),
            LongArray::class to typeOf<Long>(),
            ShortArray::class to typeOf<Short>(),
        )

        /** Creates a [CollectionAwaker] that produces a [MutableList]. */
        fun forList(type: KType) = CollectionAwaker(type.arguments[0].type!!) {
            toMutableList()
        }

        /** Creates a [CollectionAwaker] that produces a [MutableSet]. */
        fun forSet(type: KType) = CollectionAwaker(type.arguments[0].type!!) {
            toMutableSet()
        }

        /**
         * Creates a [CollectionAwaker] that produces a correctly typed array.
         *
         * Two shapes are handled:
         * - a primitive array (`IntArray`), whose JVM component type must be the PRIMITIVE class and
         *   which cannot hold nulls;
         * - `Array<T>`, whose elements are boxed, so the component type is the OBJECT class —
         *   `Array<Int>` is `Integer[]`, not `int[]`.
         */
        fun forArray(type: KType): CollectionAwaker {
            val cls = type.classifier as KClass<*>

            val primitiveElement = primitiveArrayElements[cls]

            if (primitiveElement != null) {
                val component = (primitiveElement.classifier as KClass<*>).javaPrimitiveType!!

                // A null element needs no guard here: the element types are non-nullable, so
                // SlumberModule.wrapIfNonNull already wraps their awakers in NonNullAwaker, which
                // reports the error with the element path before the array is ever written to.
                return CollectionAwaker(primitiveElement) {
                    toJavaArray(component)
                }
            }

            val element = type.arguments[0].type!!
            val component = (element.classifier as KClass<*>).javaObjectType

            return CollectionAwaker(element) {
                toJavaArray(component)
            }
        }

        /** True when [cls] is any array class, primitive or object. */
        fun isArrayClass(cls: KClass<*>): Boolean = cls.java.isArray

        private fun List<*>.toJavaArray(componentType: Class<*>): Any {
            val result = JavaArray.newInstance(componentType, size)

            forEachIndexed { idx, value -> JavaArray.set(result, idx, value) }

            return result
        }
    }

    override fun awake(data: Any?, context: Awaker.Context): Any? = when {

        data == null -> null

        data is Iterable<*> -> awakeInternal(data.toList(), context)

        // Any array shape, primitive included — see CollectionSlumberer for why `is Array<*>` is not enough.
        data::class.java.isArray -> awakeInternal(data.readArrayElements(), context)

        else -> null
    }

    private fun awakeInternal(data: List<*>, context: Awaker.Context): Any {

        return data
            .mapIndexed { idx, item ->
                context.stepInto(idx.toString()).awake(innerType, item)
            }
            .creator()
    }

    /** Reads any array — object or primitive — element by element, boxing primitives on the way out. */
    private fun Any.readArrayElements(): List<Any?> {
        val size = JavaArray.getLength(this)

        return (0 until size).map { JavaArray.get(this, it) }
    }
}
