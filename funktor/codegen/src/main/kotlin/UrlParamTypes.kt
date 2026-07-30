package io.peekandpoke.funktor.codegen

import io.peekandpoke.ultra.codegen.model.TsUrlParamClaims
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.full.withNullability

/**
 * Maps a Kotlin parameter type to the TypeScript type it has ON THE WIRE.
 *
 * A URL parameter is not sent as JSON — `OutgoingConverter` turns every value into a String
 * (`funktor/core/src/jvmMain/kotlin/broker/OutgoingConverter.kt:29`), and `buildUrl` percent-encodes
 * it. So a parameter's TypeScript type describes what the CALLER may pass, not the JSON shape of the
 * Kotlin type: `Stored<Event>` travels as the entity's id.
 *
 * Only types whose wire form is PROVABLE are mapped. Everything else returns `null` and the caller
 * refuses by name — a maintainer decision, recorded in
 * `.claude/tasks/20260730-funktor-codegen-rest-contributor.md` §4. Guessing `string` for anything the
 * converter happens to accept would type-check and then send the wrong text.
 *
 * This lives on the funktor side deliberately: "what a Kotlin type looks like in a URL" is a property
 * of funktor's converters, not of TypeScript emission.
 */
internal object UrlParamTypes {

    /** A mapped parameter type, and how its text must be formatted. */
    data class Mapped(val tsType: String, val format: String?)

    /**
     * The TypeScript type for a parameter of type [type], or `null` when it is not mappable.
     *
     * [claims] covers what reflection cannot decide: `MpInstant` is `string` in a URL only because
     * funktor registers a converter pair for it, and `Stored<T>` is the entity's id for the same
     * reason. Neither fact is visible in the Kotlin type.
     */
    fun of(type: KType, claims: TsUrlParamClaims): Mapped? {
        val notNull = type.withNullability(false)

        val mapped = claimed(notNull, claims) ?: baseOf(notNull, claims)?.let { Mapped(it, null) }

        return mapped?.let {
            if (type.isMarkedNullable) it.copy(tsType = "${it.tsType} | null") else it
        }
    }

    /**
     * A claim for [type]'s class, matched by CLASS so one claim covers every instantiation.
     *
     * Claims are consulted BEFORE the built-in mapping, so a contributor can describe a type
     * reflection would otherwise get wrong — a value class whose converter does not simply unwrap it,
     * for instance.
     */
    private fun claimed(type: KType, claims: TsUrlParamClaims): Mapped? =
        (type.classifier as? KClass<*>)
            ?.let { claims.find(it) }
            ?.let { Mapped(tsType = it.tsType, format = it.format) }

    private fun baseOf(type: KType, claims: TsUrlParamClaims): String? {
        val cls = type.classifier as? KClass<*> ?: return null

        return when {
            cls in STRING_LIKE -> "string"

            // Long included deliberately. Above 2^53 a JS number cannot hold it exactly — but the
            // caller is producing the value, not parsing one, so this is the same exposure the
            // hand-written client has. The JSON side is covered by the model's `longValued` advisory.
            cls in NUMBER_LIKE -> "number"

            cls == Boolean::class -> "boolean"

            // A union of the constant names, which is what the converter writes. Self-contained on
            // purpose: a param-only enum would otherwise have to be declared in models.ts and
            // imported, for a type that is three words long.
            cls.java.isEnum -> cls.java.enumConstants
                ?.filterIsInstance<Enum<*>>()
                ?.takeIf { it.isNotEmpty() }
                ?.joinToString(" | ") { "'${it.name}'" }

            // A value class travels as its single underlying value, so it maps to whatever that maps
            // to. Recursive, because a value class may wrap another.
            cls.isValue -> cls.primaryConstructor
                ?.parameters
                ?.singleOrNull()
                ?.type
                ?.let { of(it, claims)?.tsType }

            else -> null
        }
    }

    private val STRING_LIKE: Set<KClass<*>> = setOf(String::class, Char::class)

    private val NUMBER_LIKE: Set<KClass<*>> = setOf(
        Int::class, Long::class, Short::class, Byte::class, Float::class, Double::class,
    )
}
