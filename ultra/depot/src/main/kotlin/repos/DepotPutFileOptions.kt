package io.peekandpoke.ultra.depot.repos

import io.peekandpoke.ultra.depot.domain.DepotMeta

/** Creates [DepotPutFileOptions] via a builder DSL. */
fun depotPutFileOptions(block: DepotPutFileOptions.Builder.() -> Unit = {}) =
    DepotPutFileOptions.Builder().apply(block).build()

/** Options for writing a file to a depot repository (metadata, optional encryption). */
data class DepotPutFileOptions(
    val meta: DepotMeta,
    val encryption: Encryption?,
) {
    companion object {
        val default: DepotPutFileOptions = depotPutFileOptions()
    }

    /** Encryption algorithm to apply to the stored file. */
    sealed class Encryption {

        abstract val algoName: String

        data class AesGcmNoPadding(
            val keySize: Int = 256,
        ) : Encryption() {
            companion object {
                const val algoName = "AES/GCM/NoPadding"
            }

            override val algoName = AesGcmNoPadding.algoName
        }
    }

    class Builder internal constructor() {
        private var meta: DepotMeta = DepotMeta.default

        private var encryption: Encryption? = null

        internal fun build() = DepotPutFileOptions(
            meta = meta,
            encryption = encryption,
        )

        fun meta(meta: DepotMeta) {
            this.meta = meta
        }

        fun encryption(encryption: Encryption) {
            this.encryption = encryption
        }
    }
}
