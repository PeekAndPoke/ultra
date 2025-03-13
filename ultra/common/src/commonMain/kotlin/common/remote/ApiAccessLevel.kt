package de.peekandpoke.ultra.common.remote

enum class ApiAccessLevel {
    Granted,
    Partial,
    Denied;

    fun isGranted() = this == Granted
    fun isPartial() = this == Partial
    fun isDenied() = this == Denied

    infix fun and(other: ApiAccessLevel) = maxOf(this, other)

    infix fun or(other: ApiAccessLevel) = minOf(this, other)
}
