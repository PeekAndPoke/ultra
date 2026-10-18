package io.peekandpoke.karango.config

import io.peekandpoke.ultra.common.model.Redacted


/**
 * Configuration for connecting to an ArangoDB instance.
 *
 * Supports SSL/TLS with optional X.509 certificate pinning via [caCertX509].
 */
data class ArangoDbConfig(
    val user: String = "root",
    val password: Redacted<String> = Redacted(""),
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
