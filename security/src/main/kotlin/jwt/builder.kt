package de.peekandpoke.ultra.security.jwt

import io.jsonwebtoken.JwtBuilder
import java.util.*
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

/**
 * Set the expiration to [duration] from now on
 */

@ExperimentalTime
fun JwtBuilder.expiresIn(duration: Duration) = apply {
    setExpiration(
        Date(System.currentTimeMillis() + duration.inMilliseconds.toLong())
    )
}
