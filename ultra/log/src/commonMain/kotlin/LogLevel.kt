package io.peekandpoke.ultra.log

/**
 * Defines the available log levels ordered by [severity].
 *
 * Higher severity values indicate more critical log levels. [OFF] and [ALL] are sentinel
 * thresholds ("log nothing" / "log everything") rather than levels a message is logged at.
 *
 * The severity ordering is: [OFF] > [ERROR] > [WARNING] > [INFO] > [DEBUG] > [TRACE] > [ALL].
 *
 * @property severity the numeric severity of this level; higher values are more critical.
 *   Enum declaration order is the reverse of ascending severity, so compare via [severity] -
 *   never via `<`/`compareTo`, which use ordinal order.
 */
enum class LogLevel(val severity: Int) {
    /** Threshold meaning "log nothing": as a minimum level it drops every event; an event logged at OFF is dropped. */
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

    /** Threshold meaning "log everything": as a minimum level it drops nothing; an event logged at ALL is dropped. */
    ALL(0),
}
