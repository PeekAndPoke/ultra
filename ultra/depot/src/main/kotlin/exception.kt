package io.peekandpoke.ultra.depot

/** Thrown when a depot file operation fails. Its message never carries a caller's path. */
open class DepotException(message: String) : RuntimeException(message)

/** Thrown for a write for a data subject that was erased: nothing is stored for it any more. */
class DepotSubjectErasedException : DepotException("The data subject was erased")

/**
 * Thrown when an encrypted file or its metadata is not what was written: it was altered, moved to another path or
 * subject, or names a key that never existed. Not for an erased key, which reads as shredded.
 */
class DepotCorruptedException : DepotException("A depot file is corrupted")
