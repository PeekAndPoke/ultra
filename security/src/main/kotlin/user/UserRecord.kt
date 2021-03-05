package de.peekandpoke.ultra.security.user

data class UserRecord(
    val userId: String = "anonymous",
    val clientIp: String = "unknown",
    val desc: String = "n/a",
    val type: String = "n/a",
)
