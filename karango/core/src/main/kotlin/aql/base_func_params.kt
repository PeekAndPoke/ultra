package io.peekandpoke.karango.aql

import io.peekandpoke.ultra.vault.lang.VaultInputValueMarker

enum class AqlPercentileMethod(val method: String) {
    @VaultInputValueMarker
    RANK("rank"),

    @VaultInputValueMarker
    INTERPOLATION("interpolation")
}
