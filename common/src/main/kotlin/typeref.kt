package de.peekandpoke.ultra.common

import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.WildcardType
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType

@Suppress("unused")
data class TypeRef<T>(val type: KType) {

    companion object {

        val Any = kType<Any>()
        val AnyNull = kType<Any?>()

        val Boolean = kType<Boolean>()
        val BooleanNull = kType<Boolean?>()

        val Byte = kType<Byte>()
        val ByteNull = kType<Byte?>()

        val Char = kType<Char>()
        val CharNull = kType<Char?>()

        val Double = kType<Double>()
        val DoubleNull = kType<Double?>()

        val Float = kType<Float>()
        val FloatNull = kType<Float?>()

        val Int = kType<Int>()
        val IntNull = kType<Int?>()

        val Long = kType<Long>()
        val LongNull = kType<Long?>()

        val Number = kType<Number>()
        val NumberNull = kType<Number?>()

        val Short = kType<Short>()
        val ShortNull = kType<Short?>()

        val String = kType<String>()
        val StringNull = kType<String?>()
    }

    /**
     * Converts to a nullable type
     */
    val nullable: TypeRef<T?> by lazy(LazyThreadSafetyMode.NONE) {
        TypeRef<T?>(
            type.classifier!!.createType(
                arguments = type.arguments,
                nullable = true
            )
        )
    }

    /**
     * Wraps the current type as a [List] type
     */
    val list: TypeRef<List<T>> by lazy(LazyThreadSafetyMode.NONE) {
        TypeRef<List<T>>(
            List::class.createType(
                arguments = listOf(
                    KTypeProjection.invariant(type)
                ),
                nullable = false
            )
        )
    }

    /**
     * Wraps the current type with the given generic type [W]
     *
     * E.g. makes a String? type a MyWrapper<String?> type
     *
     * When the given type [W] does not have exactly one type parameter an exception is thrown
     */
    inline fun <reified W : Any> wrapWith(): TypeRef<W> = wrapWith(W::class, null is W)

    /**
     * Wraps the current type with the given generic type [W]
     *
     * E.g. makes a String? type a MyWrapper<String?> type
     *
     * When the given type [W] does not have exactly one type parameter an exception is thrown
     */
    @Suppress("UNCHECKED_CAST")
    fun <W : Any> wrapWith(cls: KClass<W>, nullable: Boolean): TypeRef<W> = wrapCache.getOrPut(cls) {

        if (cls.typeParameters.size != 1) {
            error("Can only wrap with a generic type with exactly one type parameter")
        }

        return TypeRef(
            cls.createType(
                arguments = listOf(
                    KTypeProjection.invariant(type)
                ),
                nullable = nullable
            )
        )
    } as TypeRef<W>


    /**
     * Internal helper val, that does not carry the correct generic type information.
     *
     * @see [unList] which takes care of the generic type information
     */
    internal val unlisted: TypeRef<Any> by lazy(LazyThreadSafetyMode.NONE) {
        if (type.classifier != List::class) {
            error("The current type [$this] must have the classifier List::class")
        }

        TypeRef<Any>(type.arguments[0].type!!)
    }

    /**
     * Internal cache used by [wrapWith]
     */
    private val wrapCache = mutableMapOf<KClass<*>, TypeRef<*>>()
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


/**
 * Creates a [TypeRef] from the given class [T]
 *
 * The given type [T] must not be generic, otherwise an exception will be thrown
 */
inline fun <reified T : Any?> kType(): TypeRef<T> {

    val cls = T::class

    if (cls.typeParameters.isNotEmpty()) {
        val tr = object : TypeReference<T>(
            cls.java.classLoader ?: Thread.currentThread().contextClassLoader
        ) {}

        return TypeRef(tr.toKType())
    }

    return TypeRef(cls.createType(nullable = null is T))
}

/**
 * Creates a [TypeRef] from the given [Class]
 */
fun <T : Any> Class<T>.kType(): TypeRef<T> = kotlin.kType()

/**
 * Creates a [TypeRef] from the given [KClass]
 */
fun <T : Any> KClass<T>.kType(): TypeRef<T> = TypeRef(
    createType(
        arguments = typeParameters.map {
            KTypeProjection.invariant(Any::class.createType())
        }
    )
)

/**
 * Creates a [TypeRef] for a List type of the given type
 */
inline fun <reified T> kListType(): TypeRef<List<T>> = kType<T>().list

/**
 * Creates a [TypeRef] for a Map type with the given [KEY] and [VAL] types
 */
inline fun <reified KEY, reified VAL> kMapType(): TypeRef<Map<KEY, VAL>> = TypeRef(
    Map::class.createType(
        arguments = listOf(
            KTypeProjection.invariant(kType<KEY>().type),
            KTypeProjection.invariant(kType<VAL>().type)
        )
    )
)

/**
 * Creates a [TypeRef] for a MutableMap type with the given [KEY] and [VAL] types
 */
inline fun <reified KEY, reified VAL> kMutableMapType(): TypeRef<Map<KEY, VAL>> = TypeRef(
    MutableMap::class.createType(
        arguments = listOf(
            KTypeProjection.invariant(kType<KEY>().type),
            KTypeProjection.invariant(kType<VAL>().type)
        )
    )
)

/**
 * Taken from Jackson 2.9.9
 */
abstract class TypeReference<T> protected constructor(
    private val classLoader: ClassLoader
) : Comparable<TypeReference<T>> {

    private val type: Type

    init {
        val superClass = javaClass.genericSuperclass

        require(superClass !is Class<*>) {
            // sanity check, should never happen
            "Internal error: TypeReference constructed without actual type information"
        }

        type = (superClass as ParameterizedType).actualTypeArguments[0]
    }

    open fun getType() = type

    /**
     * The only reason we define this method (and require implementation
     * of `Comparable`) is to prevent constructing a
     * reference without type information.
     */
    override fun compareTo(other: TypeReference<T>): Int {
        return 0
    }
    // just need an implementation, not a good one... hence ^^^

    /**
     * Returns the resulting type as a [KType]
     */
    fun toKType(): KType = type.toKType()

    /**
     * Internal helper for creating a [KType] from a [Type]
     */
    private fun Type.toKType(): KType = when (this) {

        is ParameterizedType -> toClass().createType(
            arguments = actualTypeArguments.map {
                KTypeProjection.invariant(it.toKType())
            },
            nullable = false // This is not so good. But there is currently no way to get the nullability
        )

        is WildcardType -> toClass().createType(
            arguments = upperBounds[0].toKType().arguments.map { KTypeProjection.invariant(it.type!!) },
            nullable = false // This is not so good. But there is currently no way to get the nullability
        )

        else -> toClass().createType(
            arguments = listOf(),
            nullable = false // This is not so good. But there is currently no way to get the nullability
        )
    }

    /**
     * Internal helper for creating a [KClass] from the a [Type]
     */
    private fun Type.toClass(): KClass<*> = when (this) {
        is ParameterizedType -> classForName(this.rawType.typeName)
        is WildcardType -> this.upperBounds[0].toClass()
        else -> classForName(this.typeName)
    }

    /**
     * Loads a class for the given [fqn] with the given [classLoader]
     */
    private fun classForName(fqn: String): KClass<*> =
        Class.forName(fqn, true, classLoader).kotlin
}

