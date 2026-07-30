package io.peekandpoke.funktor.core.broker.vault

import io.peekandpoke.funktor.core.broker.IncomingParamConverter
import io.peekandpoke.funktor.core.broker.OutgoingParamConverter
import io.peekandpoke.ultra.reflection.ReifiedKType
import java.lang.reflect.InvocationTargetException
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaConstructor

/**
 * True for a user-defined `@JvmInline value class` (excludes kotlin stdlib value classes like
 * `Duration`/`UInt`, exactly as slumber's `isUserValueClass` does — those would need a dedicated codec).
 */
private fun KType.isUserValueClass(): Boolean {
    val cls = classifier as? KClass<*> ?: return false
    return cls.isValue && cls.qualifiedName?.startsWith("kotlin.") != true
}

/**
 * Generic incoming param converter for user-defined `@JvmInline value class`es (e.g. a `RealmId` over a
 * `String`, or an id over a `Long`). Converts the raw URL param string to the value class's underlying
 * scalar via the [IncomingPrimitiveConverter], then constructs the value class through its primary
 * constructor — which runs any `init { require(...) }` invariant, so a malformed id is rejected at the
 * request boundary. One converter handles every scalar-backed id, so wrapping a new id in a value class
 * needs no per-type converter.
 *
 * NOTE: the inner scalar is converted by the primitive converter, so only value classes over a type it
 * supports (String/Int/Long/.../enum) bind — that covers every id in the migration. A value class over
 * a non-primitive backing would fail loudly at bind time.
 */
class IncomingValueClassConverter(
    private val primitive: IncomingPrimitiveConverter,
) : IncomingParamConverter {

    // Reflection is resolved once per value-class type (this converter runs on every request that binds
    // a value-class route param), mirroring how the slumber codecs cache their ReifiedKType per type.
    private val reifiedCache = ConcurrentHashMap<KType, ReifiedKType>()

    override fun canHandle(type: KType): Boolean = type.isUserValueClass()

    override suspend fun convert(value: String, type: KType): Any? {
        val reified = reifiedCache.getOrPut(type) {
            ReifiedKType(type).also {
                // Value classes have a synthetic private ctor — make it callable (once, when cached).
                it.ctor?.isAccessible = true
                it.ctor?.javaConstructor?.isAccessible = true
            }
        }
        val ctor = reified.ctor ?: return null
        val (param, innerType) = reified.ctorParams2Types.first()

        // Convert the raw string to the underlying scalar (String/Int/Long/... or enum).
        val inner = primitive.convert(value, innerType)

        return try {
            ctor.callBy(mapOf(param to inner))
        } catch (e: InvocationTargetException) {
            // A value class's `init` throws inside the ctor, which reflection wraps in
            // InvocationTargetException. Surface it as IllegalArgumentException so the IncomingConverter
            // turns ANY invalid value-class param into a 404 (require -> IAE; check -> ISE; ...), never
            // a 500.
            val cause = e.cause ?: e
            throw cause as? IllegalArgumentException ?: IllegalArgumentException(cause.message, cause)
        }
    }
}

/**
 * Generic outgoing param converter for user-defined `@JvmInline value class`es: unwraps to the
 * underlying scalar and stringifies it, so a rendered URL reads `.../b2b/...` rather than the value
 * class's default `RealmId(value=b2b)` toString. Required for the [io.peekandpoke.funktor.core.broker.TypedRoute]
 * converter boot-check to pass for a value-class route param.
 */
class OutgoingValueClassConverter : OutgoingParamConverter {

    private val reifiedCache = ConcurrentHashMap<KType, ReifiedKType>()

    override fun canHandle(type: KType): Boolean = type.isUserValueClass()

    override fun convert(value: Any, type: KType): String {
        val reified = reifiedCache.getOrPut(type) { ReifiedKType(type) }
        val field = reified.ctorFields2Types.first().first
        return field.get(value).toString()
    }
}
