package io.peekandpoke.ultra.slumber.builtin.polymorphism

import io.peekandpoke.ultra.slumber.Slumberer
import kotlin.reflect.KClass

/**
 * Serializes a polymorphic parent type by appending the [discriminator] field
 * with the child class identifier to the slumbered result map.
 *
 * Used only when the DECLARED slumber target is the parent type; the delegated
 * `context.slumber(data)` re-dispatches on the runtime class, which normally lands on
 * [PolymorphicChildSlumberer] and writes the same pair a second time.
 */
class PolymorphicParentSlumberer(
    private val discriminator: String,
    private val map: Map<KClass<*>, String>
) : Slumberer {
    // TODO(scan): two defects share this body.
    //  1. An unregistered runtime class gives `type == null`, and the `plus` below then OVERWRITES the
    //     correct identifier the child slumberer just wrote with `discriminator to null`.
    //  2. When `data::class` is itself a polymorphic parent (an instantiable, non-sealed
    //     Polymorphic.Parent), `context.slumber(data)` resolves back to this same slumberer and
    //     recurses until StackOverflowError.
    override fun slumber(data: Any?, context: Slumberer.Context): Any? {

        if (data == null) {
            return null
        }

        // get the type identifier
        val type = map[data::class]

        return when (val result = context.slumber(data)) {
            is Map<*, *> -> result.plus(discriminator to type)
            else -> result
        }
    }
}
