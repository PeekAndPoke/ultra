package de.peekandpoke.ultra.security.jwt

import com.auth0.jwt.exceptions.JWTDecodeException
import com.auth0.jwt.interfaces.Claim
import java.util.*

object JwtNullClaim : Claim {
    override fun isNull(): Boolean = true
    override fun isMissing(): Boolean = true

    override fun asBoolean(): Boolean? = null
    override fun asInt(): Int? = null
    override fun asLong(): Long? = null
    override fun asDouble(): Double? = null
    override fun asString(): String? = null
    override fun asDate(): Date? = null
    override fun <T : Any?> asArray(clazz: Class<T>?): Array<T>? = null
    override fun <T : Any?> asList(clazz: Class<T>?): MutableList<T>? = null
    override fun asMap(): MutableMap<String, Any>? = null

    override fun <T : Any?> `as`(clazz: Class<T>?): T {
        throw JWTDecodeException("Cannot be converted")
    }
}
