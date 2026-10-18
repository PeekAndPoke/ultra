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
// TODO(scan): OFF/ALL are documented as suppressing/enabling all logging, but nothing in this
//  module actually filters by severity - UltraLogManager.log() and ConsoleAppender.append()
//  forward every level unconditionally, so LogLevel.OFF still gets printed.
enum class LogLevel(val severity: Int) {
    /** Sentinel meaning "suppress everything"; not enforced by this module - see TODO above. */
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

    /** Sentinel meaning "log everything"; not enforced by this module - see TODO above. */
    ALL(0),
}
