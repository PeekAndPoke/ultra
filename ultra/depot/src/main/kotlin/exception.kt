package io.peekandpoke.ultra.depot

/** Thrown when a depot file operation fails. Its message never carries a caller's path. */
class DepotException(message: String) : RuntimeException(message)
