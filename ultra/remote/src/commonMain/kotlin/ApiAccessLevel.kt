package io.peekandpoke.ultra.remote

/**
 * Represents the access level granted by an API authorization check.
 *
 * The enum values are ordered from least restrictive to most restrictive:
 * [Granted] < [Partial] < [Denied].
 *
 * The [and] operator returns the **most** restrictive of two levels (logical AND),
 * while [or] returns the **least** restrictive (logical OR).
 */
enum class ApiAccessLevel {
    /** Full access is granted. */
    Granted,

    /** Partial access is granted. */
    Partial,

    /** Access is denied. */
    Denied;

    /** Returns `true` when this level is [Granted]. */
    fun isGranted() = this == Granted

    /** Returns `true` when this level is [Partial]. */
    fun isPartial() = this == Partial

    /** Returns `true` when this level is [Denied]. */
    fun isDenied() = this == Denied

    /**
     * Combines two access levels by returning the **most restrictive** one.
     *
     * Useful for requiring **all** conditions to pass.
     */
    infix fun and(other: ApiAccessLevel) = maxOf(this, other)

    /**
     * Combines two access levels by returning the **least restrictive** one.
     *
     * Useful for requiring **any** condition to pass.
     */
    infix fun or(other: ApiAccessLevel) = minOf(this, other)
}
