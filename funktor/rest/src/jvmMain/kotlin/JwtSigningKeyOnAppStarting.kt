package io.peekandpoke.funktor.rest

import io.ktor.server.application.Application
import io.peekandpoke.funktor.core.lifecycle.AppLifeCycleHooks
import io.peekandpoke.ultra.log.Log
import io.peekandpoke.ultra.security.jwt.JwtConfig

/**
 * Logs once, at boot, which key signs new tokens (ULTRA-00178). `keys.first()` signs, so a new key APPENDED instead of
 * prepended boots clean and goes on signing with the old one; this line makes that visible after a deploy. No boot
 * check could refuse it: the two-phase rotation keeps the older key first on purpose while the new one spreads.
 *
 * Names the key id, the algorithm and `issued`; never any part of the secret, not even its length. Silent where the
 * kontainer has no [Log].
 */
class JwtSigningKeyOnAppStarting(
    private val config: JwtConfig,
    private val log: Log?,
) : AppLifeCycleHooks.OnAppStarting {

    override suspend fun onAppStarting(application: Application) {
        announce()
    }

    /** Writes the boot line, at INFO. */
    internal fun announce() {
        log?.info(message(config))
    }

    companion object {
        /** The boot line for [config]. */
        internal fun message(config: JwtConfig): String {
            val signing = config.keys.first()
            val issued = signing.issued?.let { ", issued $it" } ?: ""

            return "JWT: new tokens are signed with kid '${signing.id}' (${signing.alg.headerValue}$issued); " +
                    "${config.keys.size} key(s) verify"
        }
    }
}
