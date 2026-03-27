package io.peekandpoke.ultra.reflection

import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType

/**
 * Creates a type safe representation of the given [KType]
 *
 * [TypeRef] is a wrapper around KType. It also carries type information in [T].
 *
 * By doing so we can utilize the compiler for checking that reflection types are used correctly.
 */
@ConsistentCopyVisibility
data class TypeRef<T> private constructor(val type: KType) {

    companion object {

        /** Cache for [KType] to [TypeRef] */
        private val cachedKTypes = mutableMapOf<KType, TypeRef<*>>()

        /** Cache for [KClass] to [TypeRef] */
        private val cachedNullableKClasses = mutableMapOf<KClass<*>, TypeRef<*>>()

        /** Cache for [KClass] to [TypeRef] */
        private val cachedNonNullKClasses = mutableMapOf<KClass<*>, TypeRef<*>>()

        /**
         * Creates or retrieves a cached [TypeRef] for the given [KType].
         *
         * This is the primary factory for constructing a [TypeRef] from an already-known [KType].
         */
        fun <T> createForKType(type: KType): TypeRef<T> {
            @Suppress("UNCHECKED_CAST")
            return cachedKTypes.getOrPut(type) {
                TypeRef<T>(type)
            } as TypeRef<T>
        }

        /**
         * Creates or retrieves a cached [TypeRef] for the given [KClass].
         *
         * Generic type parameters of the class are filled with `Any` projections.
         * Set [nullable] to `true` to produce a nullable type.
         */
        fun <T> createForKClass(cls: KClass<*>, nullable: Boolean): TypeRef<T> {

            val cache = if (nullable) {
                cachedNullableKClasses
            } else {
                cachedNonNullKClasses
            }

            @Suppress("UNCHECKED_CAST")
            return cache.getOrPut(cls) {

                val type = cls.createType(
                    arguments = cls.typeParameters.map {
                        KTypeProjection.invariant(kotlin.Any::class.createType())
                    }
                )
                // Return
                createForKType<T>(type)
            } as TypeRef<T>
        }

        /** Pre-built [TypeRef] for [kotlin.Unit]. */
        val Unit = kType<Unit>()

        /** Pre-built nullable [TypeRef] for [kotlin.Unit]?. */
        val UnitNull = kType<Unit?>()

        /** Pre-built [TypeRef] for [kotlin.Any]. */
        val Any = kType<Any>()

        /** Pre-built nullable [TypeRef] for [kotlin.Any]?. */
        val AnyNull = kType<Any?>()

        /** Pre-built [TypeRef] for [kotlin.Boolean]. */
        val Boolean = kType<Boolean>()

        /** Pre-built nullable [TypeRef] for [kotlin.Boolean]?. */
        val BooleanNull = kType<Boolean?>()

        /** Pre-built [TypeRef] for [kotlin.Byte]. */
        val Byte = kType<Byte>()

        /** Pre-built nullable [TypeRef] for [kotlin.Byte]?. */
        val ByteNull = kType<Byte?>()

        /** Pre-built [TypeRef] for [kotlin.Char]. */
        val Char = kType<Char>()

        /** Pre-built nullable [TypeRef] for [kotlin.Char]?. */
        val CharNull = kType<Char?>()

        /** Pre-built [TypeRef] for [kotlin.Double]. */
        val Double = kType<Double>()

        /** Pre-built nullable [TypeRef] for [kotlin.Double]?. */
        val DoubleNull = kType<Double?>()

        /** Pre-built [TypeRef] for [kotlin.Float]. */
        val Float = kType<Float>()

        /** Pre-built nullable [TypeRef] for [kotlin.Float]?. */
        val FloatNull = kType<Float?>()

        /** Pre-built [TypeRef] for [kotlin.Int]. */
        val Int = kType<Int>()

        /** Pre-built nullable [TypeRef] for [kotlin.Int]?. */
        val IntNull = kType<Int?>()

        /** Pre-built [TypeRef] for [kotlin.Long]. */
        val Long = kType<Long>()

        /** Pre-built nullable [TypeRef] for [kotlin.Long]?. */
        val LongNull = kType<Long?>()

        /** Pre-built [TypeRef] for [kotlin.Number]. */
        val Number = kType<Number>()

        /** Pre-built nullable [TypeRef] for [kotlin.Number]?. */
        val NumberNull = kType<Number?>()

        /** Pre-built [TypeRef] for [kotlin.Short]. */
        val Short = kType<Short>()

        /** Pre-built nullable [TypeRef] for [kotlin.Short]?. */
        val ShortNull = kType<Short?>()

        /** Pre-built [TypeRef] for [kotlin.String]. */
        val String = kType<String>()

        /** Pre-built nullable [TypeRef] for [kotlin.String]?. */
        val StringNull = kType<String?>()
    }

    /**
     * Returns a [ReifiedKType] of this TypeRef
     */
    val reified: ReifiedKType by lazy { ReifiedKType(type) }

    /**
     * Converts to a nullable type
     */
    val nullable: TypeRef<T?> by lazy(LazyThreadSafetyMode.NONE) {
        @Suppress("RemoveExplicitTypeArguments")
        (TypeRef<T?>(
            type.classifier!!.createType(
                arguments = type.arguments,
                nullable = true
            )
        ))
    }

    /**
     * Wraps the current type as a [List] type
     */
    val list: TypeRef<List<T>> by lazy(LazyThreadSafetyMode.NONE) {
        @Suppress("RemoveExplicitTypeArguments")
        (createForKType<List<T>>(
            List::class.createType(
                arguments = listOf(KTypeProjection.invariant(type)),
                nullable = false
            )
        ))
    }

    /**
     * Wraps the current type with the given generic type [W]
     *
     * E.g. makes a String? type a MyWrapper<String?> type
     *
     * When the given type [W] does not have exactly one type parameter an exception is thrown
     */
    inline fun <reified W : Any> wrapWith(): TypeRef<W> {
        @Suppress("USELESS_IS_CHECK")
        return wrapWith(cls = W::class, null is W)
    }

    /**
     * Wraps the current type with the given generic type [W]
     *
     * E.g. makes a String? type a MyWrapper<String?> type
     *
     * When the given type [W] does not have exactly one type parameter an exception is thrown
     */
    @Suppress("UNCHECKED_CAST")
    fun <W : Any> wrapWith(cls: KClass<W>, nullable: Boolean): TypeRef<W> {

        if (cls.typeParameters.size != 1) {
            error("Can only wrap with a generic type with exactly one type parameter")
        }

        val cache = if (nullable) {
            nullableWrapCache
        } else {
            nonNullWrapCache
        }

        return cache.getOrPut(cls) {
            createForKType<W>(
                cls.createType(
                    arguments = listOf(KTypeProjection.invariant(type)),
                    nullable = nullable
                )
            )
        } as TypeRef<W>
    }

    /**
     * Internal helper val, that does not carry the correct generic type information.
     *
     * @see [unList] which takes care of the generic type information
     */
    internal val unlisted: TypeRef<Any> by lazy(LazyThreadSafetyMode.NONE) {
        if (type.classifier != List::class) {
            error("The current type [$this] must have the classifier List::class")
        }

        @Suppress("RemoveExplicitTypeArguments")
        (TypeRef<Any>(type.arguments[0].type!!))
    }

    /**
     * Internal cache used by [wrapWith]
     */
    private val nonNullWrapCache = mutableMapOf<KClass<*>, TypeRef<*>>()

    /**
     * Internal cache used by [wrapWith]
     */
    private val nullableWrapCache = mutableMapOf<KClass<*>, TypeRef<*>>()
}

/**
 * Unwraps a List-type
 *
 * E.g. makes a List<String?>-type a String?-type.
 *
 * When the classifier of the current type is not List::class an exception is thrown.
 */
@Suppress("UNCHECKED_CAST")
val <T> TypeRef<List<T>>.unList: TypeRef<T>
    get() = unlisted as TypeRef<T>
