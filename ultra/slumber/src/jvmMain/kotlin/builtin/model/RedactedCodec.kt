package io.peekandpoke.ultra.slumber.builtin.model

import io.peekandpoke.ultra.common.model.Redacted
import io.peekandpoke.ultra.slumber.Awaker
import io.peekandpoke.ultra.slumber.AwakerException
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

        // FAIL LOUD on our own output. Reading the placeholder back would produce a Redacted holding
        // the literal "***redacted***" — a real object carrying a publicly known constant where a
        // secret belongs. Silent, and catastrophic where it lands: a config rebuilt from an insights
        // record or an `app:config` dump would boot and sign JWTs under a value anyone can read off
        // this source file. Throwing turns that into a startup failure.
        //
        // This also covers the read-modify-write shape in general — a REST DTO or a stored entity
        // holding a Redacted, fetched and PUT back — which is why the check lives here, at the single
        // point every awake path goes through, rather than in a per-secret boot guard.
        if (data == Redacted.PLACEHOLDER) {
            throw AwakerException(
                message = "Value at path '${context.path}' is the redaction placeholder, not a secret. " +
                        "A redacted value was read back in: the original is gone and must be supplied again.",
                logs = emptyList(),
                rootType = null,
                input = null, // deliberately not echoed
            )
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
