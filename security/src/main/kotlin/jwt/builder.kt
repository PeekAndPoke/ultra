package de.peekandpoke.ultra.security.jwt

import com.auth0.jwt.JWTCreator
import java.util.*
import kotlin.time.Duration
import kotlin.time.ExperimentalTime

/**
 * Set the expiration to [duration] from now on
 */

@ExperimentalTime
fun JWTCreator.Builder.expiresIn(duration: Duration) = apply {
    withExpiresAt(
        Date(System.currentTimeMillis() + duration.inMilliseconds.toLong())
    )
}
