package de.peekandpoke.ultra.common

/**
 * If [condition] is true, returns the result of [modifier], otherwise returns this.
 */
fun <T> T.modifyIf(condition: Boolean, modifier: T.() -> T) = if (condition) modifier() else this
