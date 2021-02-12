package de.peekandpoke.ultra.security.user

data class UserRecord(
    val userId: String = "anonymous",
    val clientIp: String = "unknown"
)
