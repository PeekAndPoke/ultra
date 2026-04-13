package io.peekandpoke.funktor.core.session

import io.ktor.server.sessions.*
import kotlin.reflect.KClass

/** No-op [CurrentSession] implementation used when no session is active. */
object NullCurrentSession : CurrentSession {
    override fun clear(name: String): Unit = Unit

    override fun findName(type: KClass<*>): String = ""

    override fun get(name: String): Any? = null

    override fun set(name: String, value: Any?): Unit = Unit
}
