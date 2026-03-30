package io.peekandpoke.ultra.log

/**
 * Defines the available log levels ordered by [severity].
 *
 * Higher severity values indicate more critical log levels.
 * [OFF] suppresses all logging, while [ALL] enables everything.
 *
 * The severity ordering is: [OFF] > [ERROR] > [WARNING] > [INFO] > [DEBUG] > [TRACE] > [ALL].
 *
 * @property severity the numeric severity of this log level; higher values are more critical.
 */
enum class LogLevel(val severity: Int) {
    /** Suppresses all log output. */
    OFF(Int.MAX_VALUE),

    /** Indicates an error condition that should be investigated. */
    ERROR(500),

    /** Indicates a potentially harmful situation. */
    WARNING(400),

    /** Informational messages highlighting progress or state. */
    INFO(300),

    /** Fine-grained informational events useful for debugging. */
    DEBUG(200),

    /** Very detailed diagnostic information. */
    TRACE(100),

    /** Enables all log levels. */
    ALL(0),
}
