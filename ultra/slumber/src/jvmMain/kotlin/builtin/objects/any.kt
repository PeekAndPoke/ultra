package io.peekandpoke.ultra.slumber.builtin.objects

import io.peekandpoke.ultra.slumber.Awaker
import io.peekandpoke.ultra.slumber.Slumberer

/** Awaker for [Any] types. Returns the raw data as-is without transformation. */
object AnyAwaker : Awaker {
    override fun awake(data: Any?, context: Awaker.Context) = data
}

/** Slumberer for [Any] types. Delegates to the codec to resolve the concrete type's slumberer. */
object AnySlumberer : Slumberer {
    override fun slumber(data: Any?, context: Slumberer.Context) = context.slumber(data)
}
