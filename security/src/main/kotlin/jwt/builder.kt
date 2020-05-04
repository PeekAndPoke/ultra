package de.peekandpoke.ultra.security.jwt

import com.auth0.jwt.JWTCreator
import de.peekandpoke.ultra.common.plusMinutes
import java.util.*

/**
 * Set the expiration to [minutes] from now on
 */
fun JWTCreator.Builder.expiresInMinutes(minutes: Long) = apply {
    withExpiresAt(Date().plusMinutes(minutes))
}
