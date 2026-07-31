package io.peekandpoke.ultra.slumber.builtin.model

import io.peekandpoke.ultra.common.model.Redacted
import io.peekandpoke.ultra.slumber.Awaker
import io.peekandpoke.ultra.slumber.Slumberer
import kotlin.reflect.KType

/**
 * Awaker for [Redacted] values: awakens the inner value normally and wraps it.
 *
 * **Takes the RAW node, not a wrapper shape.** A config file says `signingKey = "abc"`, so this must
 * hand `"abc"` to the inner type's own awaker. Falling through to `DataClassAwaker` instead would demand
 * `signingKey { value = "abc" }` — i.e. **every config file in every app** would have to nest its
 * secrets a level deeper. Measured before this was written: without this awaker the natural shape fails
 * with `AwakerException: Value at path 'root.signingKey' must not be null`.
 *
 * [innerType] comes from the DECLARED type — `getAwaker` receives it, so `T` is known here even though
 * it is erased at runtime.
 */
class RedactedAwaker(private val innerType: KType) : Awaker {

    override fun awake(data: Any?, context: Awaker.Context): Redacted<Any?>? {
        // A missing value is a missing value; the wrapper does not invent one.
        if (data == null) {
            return null
        }

        return Redacted(context.awake(innerType, data))
    }
}

/**
 * Slumberer for [Redacted] values: always [Redacted.PLACEHOLDER], whatever is inside.
 *
 * The inner value is never consulted, so a `Redacted<SomeConfig>` replaces the **whole subtree** rather
 * than walking it and redacting the leaves that happen to look sensitive. That is the reason the type is
 * generic at all.
 *
 * `T` is erased here, and deliberately unused: `getSlumberer` is asked about the RUNTIME class, where
 * the type argument is gone — which costs nothing, because the output does not depend on it.
 */
object RedactedSlumberer : Slumberer {

    override fun slumber(data: Any?, context: Slumberer.Context): String? {

        if (data !is Redacted<*>) {
            return null
        }

        return Redacted.PLACEHOLDER
    }
}
