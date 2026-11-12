package io.peekandpoke.ultra.depot

/** Thrown when a depot file operation fails. Its message never carries a caller's path. */
class DepotException(message: String) : RuntimeException(message)

/**
 * Thrown when an encrypted file cannot be decrypted though its key is there: it was altered, or moved to another
 * path or subject.
 */
class DepotCorruptedException : RuntimeException("A depot file is corrupted")
