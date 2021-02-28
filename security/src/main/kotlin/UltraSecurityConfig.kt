package de.peekandpoke.ultra.security

data class UltraSecurityConfig(
    // TODO: [Issue][https://github.com/PeekAndPoke/ultra/issues/4]
    //       Add @get:JsonIgnore so the value will not be visible on logs or insights etc.
    val csrfSecret: String,
    val csrfTtlMillis: Int
)
