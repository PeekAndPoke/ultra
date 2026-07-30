package io.peekandpoke.ultra.vault

/**
 * Settings for the [Ultra_Vault] kontainer module.
 *
 * @param profile Records timings and variables for every query. Off by default, as it keeps one
 *   entry per query alive for the lifetime of the request's `QueryProfiler`.
 * @param explain Additionally asks the database to explain each query. Only takes effect when
 *   [profile] is on — otherwise the module installs the null profiler, which ignores it.
 */
data class VaultConfig(
    val profile: Boolean = false,
    val explain: Boolean = false,
) {
    companion object {
        /** Profiling and explaining both off. */
        val default = VaultConfig()
    }
}
