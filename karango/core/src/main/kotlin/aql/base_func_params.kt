package de.peekandpoke.karango.aql

import de.peekandpoke.ultra.vault.lang.VaultInputValueMarker

enum class AqlPercentileMethod(val method: String) {
    @VaultInputValueMarker
    RANK("rank"),

    @VaultInputValueMarker
    INTERPOLATION("interpolation")
}
