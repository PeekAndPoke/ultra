package io.peekandpoke.ultra.security

/** Configuration for the ultra-security module, holding CSRF secret and TTL settings. */
data class UltraSecurityConfig(
    // TODO: [Issue][https://github.com/PeekAndPoke/ultra/issues/4]
    //       Add @get:JsonIgnore so the value will not be visible on logs or insights etc.
    val csrfSecret: String,
    val csrfTtlMillis: Int,
) {
    companion object {
        /** Empty configuration with no CSRF secret and a default 5-minute TTL. */
        val empty = UltraSecurityConfig(
            csrfSecret = "",
            csrfTtlMillis = 300_000,
        )
    }
}
