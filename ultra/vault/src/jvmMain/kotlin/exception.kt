package io.peekandpoke.ultra.vault

/**
 * Signals a Vault usage error: a repository that cannot be resolved, or an entity that cannot be
 * serialized or referenced.
 *
 * Note it extends [Throwable] rather than `Exception`, so a `catch (e: Exception)` around a Vault
 * call does NOT catch it. Catch [VaultException] or [Throwable] explicitly.
 */
class VaultException(message: String, cause: Throwable? = null) : Throwable(message, cause)
