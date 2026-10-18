package io.peekandpoke.ultra.slumber.builtin.objects

import io.peekandpoke.ultra.reflection.ReifiedKType
import io.peekandpoke.ultra.slumber.Awaker
import kotlin.reflect.KType
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.javaConstructor

/**
 * Deserializes a scalar into a Kotlin `@JvmInline value class`: awakes the raw value to the value
 * class's single underlying type (via the INNER codec), then calls the primary constructor.
 *
 * Matches kotlinx.serialization's default: construct via the ctor with NO transform. Slumber has no
 * "construct via a factory" concept and does not need one — values are stored CANONICAL (normalized at
 * the write boundary, e.g. `Email.of(...)`), so what comes back is always already valid.
 *
 * SECURITY / INVARIANTS: validation or normalization that lives only in a FACTORY (`of(...)`) is
 * BYPASSED here — the ctor is called directly. An `init { require(...) }` block DOES run (it is part of
 * the ctor). So put any invariant that must survive deserialization of untrusted input in `init {}`, or
 * normalize/validate at the deserialization boundary — do NOT rely on an `of()` factory alone.
 */
interface ValueClassAwaker : Awaker {

    companion object {
        /** Creates a [ValueClassAwaker] for the given value-class [type]. */
        operator fun invoke(type: KType): ValueClassAwaker = Default(type)
    }

    private class Default(type: KType) : ValueClassAwaker {
        private val reified = ReifiedKType(type)
        private val primaryCtor = reified.ctor

        /** A value class has exactly one ctor parameter — the underlying value. */
        private val param = reified.ctorParams2Types.first().first

        /** The reified type of that parameter, so a generic value class resolves its argument. */
        private val innerType = reified.ctorParams2Types.first().second

        init {
            // Value classes usually have a synthetic/private constructor — make it callable.
            primaryCtor?.isAccessible = true
            primaryCtor?.javaConstructor?.isAccessible = true
        }

        override fun awake(data: Any?, context: Awaker.Context): Any? {
            val ctor = primaryCtor ?: return null

            // A null raw value cannot construct a value class whose underlying is non-null. Short-circuit
            // BEFORE awaking the inner: awaking `null` against a non-null inner type resolves to a
            // NonNullAwaker and would THROW here. A nullable UNDERLYING (`value class N(val v: String?)`)
            // still proceeds and constructs `N(null)`. (For a null nullable value-class TYPE, this awaker
            // is bare — not NonNull-wrapped — so returning null yields a null field.)
            // TODO(scan): the guard reads only the INNER nullability, never reified.type's. For a
            //   nullable use site of a value class over a nullable inner (`val x: N?`), null awakes to
            //   N(null) rather than null, so N(null) and an absent value are indistinguishable and the
            //   round trip breaks.
            if (data == null && !innerType.isMarkedNullable) {
                return null
            }

            // Awake the raw scalar to the underlying type — its codec selects string/number/etc.
            val inner = context.awake(innerType, data)

            // Defensive backstop: with the built-in modules a non-null inner type is NonNull-wrapped and
            // never returns null (it throws), so this is unreachable for them — it only fires if a
            // third-party SlumberModule returns an un-wrapped awaker for a non-null type.
            if (inner == null && !innerType.isMarkedNullable) {
                return null
            }

            return ctor.callBy(mapOf(param to inner))
        }
    }
}
