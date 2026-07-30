package io.peekandpoke.funktor.codegen

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

    /** The TypeScript type for a parameter of type [type], or `null` when it is not mappable. */
    fun of(type: KType): String? {
        val base = baseOf(type.withNullability(false)) ?: return null

        return if (type.isMarkedNullable) "$base | null" else base
    }

    private fun baseOf(type: KType): String? {
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
                ?.let { of(it) }

            else -> null
        }
    }

    private val STRING_LIKE: Set<KClass<*>> = setOf(String::class, Char::class)

    private val NUMBER_LIKE: Set<KClass<*>> = setOf(
        Int::class, Long::class, Short::class, Byte::class, Float::class, Double::class,
    )
}
