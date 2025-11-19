package de.peekandpoke.ultra.vault

data class VaultConfig(
    val profile: Boolean = false,
    val explain: Boolean = false,
) {
    companion object {
        val default = VaultConfig()
    }
}
