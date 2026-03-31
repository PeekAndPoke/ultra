package io.peekandpoke.ultra.security

/** Configuration for the ultra-security module, holding CSRF secret and TTL settings. */
data class UltraSecurityConfig(
    val csrfSecret: String,
    val csrfTtlMillis: Long,
) {
    companion object {
        /** Test-only configuration with a placeholder CSRF secret and a default 5-minute TTL. */
        val testOnly = UltraSecurityConfig(
            csrfSecret = "test-only-csrf-secret",
            csrfTtlMillis = 300_000L,
        )
    }

    /** Redacts [csrfSecret] to prevent accidental exposure in logs or error messages. */
    override fun toString(): String =
        "UltraSecurityConfig(csrfSecret=REDACTED, csrfTtlMillis=$csrfTtlMillis)"
}
