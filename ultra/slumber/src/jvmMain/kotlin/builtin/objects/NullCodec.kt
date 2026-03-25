package io.peekandpoke.ultra.slumber.builtin.objects

import io.peekandpoke.ultra.slumber.Awaker
import io.peekandpoke.ultra.slumber.Slumberer

/** Codec that always returns null. Used for [Nothing] and [Unit] types. */
object NullCodec : Awaker, Slumberer {

    override fun awake(data: Any?, context: Awaker.Context): Any? = null

    override fun slumber(data: Any?, context: Slumberer.Context): Any? = null
}
