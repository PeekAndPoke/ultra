package io.peekandpoke.ultra.security

/** Configuration for the ultra-security module, holding CSRF secret and TTL settings. */
data class UltraSecurityConfig(
    // TODO: [Issue][https://github.com/PeekAndPoke/ultra/issues/4]
    //       Add @get:JsonIgnore so the value will not be visible on logs or insights etc.
    val csrfSecret: String,
    val csrfTtlMillis: Int,
) {
    companion object {
        /** Test-only configuration with a placeholder CSRF secret and a default 5-minute TTL. */
        val testOnly = UltraSecurityConfig(
            csrfSecret = "test-only-csrf-secret",
            csrfTtlMillis = 300_000,
        )
    }
}
