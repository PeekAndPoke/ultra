package io.peekandpoke.ultra.slumber.builtin.polymorphism

import io.peekandpoke.ultra.slumber.Slumberer

/**
 * Serializes a specific polymorphic child type by delegating to [childSlumberer]
 * and appending the [discriminator]/[identifier] pair to the result map.
 *
 * This is the path a child takes when it is slumbered STANDALONE (as a root value, a data-class
 * field or a collection element) — those all dispatch on the runtime class, so the discriminator is
 * written without any parent slumberer being involved.
 */
// TODO(scan): `plus` lets the discriminator overwrite a same-named real property of the child — a
//  child declaring a `_type` field silently loses it, with no error and no round trip.
class PolymorphicChildSlumberer(
    private val discriminator: String,
    private val identifier: String,
    private val childSlumberer: Slumberer
) : Slumberer {

    private val disc2ident = discriminator to identifier

    override fun slumber(data: Any?, context: Slumberer.Context): Any? {

        if (data == null) {
            return null
        }

        return when (val result = childSlumberer.slumber(data, context)) {
            is Map<*, *> -> result.plus(disc2ident)
            else -> result
        }
    }
}
