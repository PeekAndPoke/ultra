package de.peekandpoke.karango.config

import com.fasterxml.jackson.annotation.JsonIgnore

/**
 * Configuration for connecting to an ArangoDB instance.
 *
 * Supports SSL/TLS with optional X.509 certificate pinning via [caCertX509].
 */
data class ArangoDbConfig(
    val user: String = "root",
    @get:JsonIgnore // Ignored so it will now show up in the logs or insights panel
    val password: String = "",
    val host: String = "localhost",
    val port: Int = 8529,
    val database: String = "_system",
    val useSsl: Boolean = false,
    val caCertX509: String? = null,
) {
    companion object {
        val forUnitTests get() = ArangoDbConfig()
    }
}
