package io.peekandpoke.ultra.slumber.builtin.objects

import io.peekandpoke.ultra.slumber.Awaker
import io.peekandpoke.ultra.slumber.Slumberer
import kotlin.reflect.KClass
import kotlin.reflect.KType

/**
 * Codec for enum types. Serializes to the enum constant's [name][Enum.name] and deserializes by an
 * exact, case-sensitive name lookup.
 *
 * Neither `SerialName` nor `AdditionalSerialName` is honoured here — those only drive polymorphic
 * type discrimination. An unknown name, or any non-`String` input, awakes to `null`, which a
 * surrounding `NonNullAwaker` turns into an exception for a non-nullable declared type.
 */
// TODO(scan): BuiltInModule selects this via `cls.java.isEnum`, which is false for the anonymous
//   subclass of an enum constant that has a class body — so slumbering such a constant by its runtime
//   class finds no slumberer at all.
class EnumCodec(type: KType) : Awaker, Slumberer {

    // TODO(scan): the @Suppress is inert (this is a plain cast) and the cast throws for a classifier
    //   that is not a KClass; enumConstants is then null for any non-enum class.
    @Suppress("UNCHECKED_CAST")
    private val cls = type.classifier as KClass<*>

    /** Lookup for enum values by string */
    private val lookUp = cls.java.enumConstants
        .mapNotNull { it as? Enum<*> }
        .map { it.name to it }
        .toMap()

    override fun awake(data: Any?, context: Awaker.Context): Any? {

        if (data !is String) {
            return null
        }

        return lookUp[data]
    }

    override fun slumber(data: Any?, context: Slumberer.Context): Any? {
        return when (data) {
            is Enum<*> -> data.name
            else -> null
        }
    }
}
