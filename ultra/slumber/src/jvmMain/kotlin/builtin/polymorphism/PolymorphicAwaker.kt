package io.peekandpoke.ultra.slumber.builtin.polymorphism

import io.peekandpoke.ultra.reflection.TypeRef
import io.peekandpoke.ultra.slumber.Awaker
import kotlin.reflect.KClass

/**
 * Deserializes polymorphic types by reading a [discriminator] field from the data map
 * and dispatching to the corresponding child class.
 *
 * [map] is a fixed identifier allow-list built at construction time (see
 * [PolymorphicParentUtil.createParentAwaker]); the data can only select from it, never name a class.
 * [default] is the fallback used when the discriminator is missing or unknown — null means "give up".
 */
class PolymorphicAwaker(
    private val discriminator: String,
    private val map: Map<String, KClass<*>>,
    private val default: KClass<*>?
) : Awaker {

    companion object {
        private val IdentifierType = TypeRef.String.nullable.type
    }

    // TODO(scan): every failure mode here returns a bare null with no `context.log { }` — non-map
    //  input, missing discriminator, unknown identifier and an empty map are indistinguishable, and
    //  the resulting AwakerException only says "must not be null" without naming the bad identifier.
    override fun awake(data: Any?, context: Awaker.Context): Any? {

        if (data !is Map<*, *>) {
            return null
        }

        // get the type
        val type = context.awake(IdentifierType, data[discriminator])

        // get the target class or the default if present
        val target = type?.let { identifier -> map[identifier] } ?: default

        // awake the target class
        return target?.let { cls -> context.awake(cls, data) }
    }
}
