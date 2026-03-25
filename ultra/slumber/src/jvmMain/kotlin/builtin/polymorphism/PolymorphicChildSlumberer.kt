package io.peekandpoke.ultra.slumber.builtin.polymorphism

import io.peekandpoke.ultra.slumber.Slumberer

/**
 * Serializes a specific polymorphic child type by delegating to [childSlumberer]
 * and appending the [discriminator]/[identifier] pair to the result map.
 */
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
