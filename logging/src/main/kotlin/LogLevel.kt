package de.peekandpoke.ultra.logging

enum class LogLevel(val severity: Int) {
    OFF(Int.MAX_VALUE),
    ERROR(500),
    WARNING(400),
    INFO(300),
    DEBUG(200),
    TRACE(100)
}
