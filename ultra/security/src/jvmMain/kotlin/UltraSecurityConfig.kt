package io.peekandpoke.ultra.security
import io.peekandpoke.ultra.common.model.Redacted

/** Configuration for the ultra-security module, holding CSRF secret and TTL settings. */
data class UltraSecurityConfig(
    val csrfSecret: Redacted<String>,
    val csrfTtlMillis: Long,
) {
    companion object {
        /** Test-only configuration with a placeholder CSRF secret and a default 5-minute TTL. */
        val testOnly = UltraSecurityConfig(
            csrfSecret = Redacted("test-only-csrf-secret"),
            csrfTtlMillis = 300_000L,
        )
    }

    // See JwtConfig: [Redacted] redacts itself, so no hand-written toString is needed.
}
