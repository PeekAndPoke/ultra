package io.peekandpoke.ultra.depot.repos

/** Marks the write that stores metadata unchecked. It is for middlewares; an app writes with `putFile`. */
@RequiresOptIn(message = "Writes the depot's own metadata entries unchecked. For middlewares only.")
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
annotation class DepotRawWrite
