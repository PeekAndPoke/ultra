package io.peekandpoke.ultra.slumber.builtin.objects

import io.peekandpoke.ultra.reflection.ReifiedKType
import io.peekandpoke.ultra.slumber.Slumberer
import kotlin.reflect.KType

/**
 * Serializes a Kotlin `@JvmInline value class` into its single underlying value, slumbered with the
 * INNER type's codec.
 *
 * Matches kotlinx.serialization exactly: a value class is emitted as a plain scalar of its underlying
 * value (`String -> "s"`, `Int -> 42`), never as an object `{ "value": ... }`.
 */
interface ValueClassSlumberer : Slumberer {

    companion object {
        /** Creates a [ValueClassSlumberer] for the given value-class [type]. */
        operator fun invoke(type: KType): ValueClassSlumberer = Default(type)
    }

    private class Default(type: KType) : ValueClassSlumberer {
        private val reified = ReifiedKType(type)

        /** A value class has exactly one underlying property (made accessible by [ReifiedKType]). */
        private val field = reified.ctorFields2Types.first().first

        // TODO(scan): dispatches on the RUNTIME class of the unwrapped value, while ValueClassAwaker
        //   awakes against the DECLARED inner type - the two disagree wherever a declared inner type
        //   would pick a different codec than the runtime class does.
        override fun slumber(data: Any?, context: Slumberer.Context): Any? {
            if (data == null) {
                return null
            }
            // Unwrap to the underlying value and slumber THAT — its runtime type selects the inner codec.
            return context.slumber(field.get(data))
        }
    }
}
