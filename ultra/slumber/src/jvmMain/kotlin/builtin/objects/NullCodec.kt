package io.peekandpoke.ultra.slumber.builtin.objects

import io.peekandpoke.ultra.slumber.Awaker
import io.peekandpoke.ultra.slumber.Slumberer

/**
 * Codec that always returns null, whatever the input. Registered for [Nothing] and [Unit].
 *
 * It is also what slumbers a `null` value, because the codec resolves a null by its stand-in class
 * [Nothing]. Neither registration is wrapped for non-nullability, so it never throws.
 */
object NullCodec : Awaker, Slumberer {

    override fun awake(data: Any?, context: Awaker.Context): Any? = null

    override fun slumber(data: Any?, context: Slumberer.Context): Any? = null
}
