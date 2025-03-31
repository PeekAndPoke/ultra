package de.peekandpoke.funktor.cluster.depot.repos

import de.peekandpoke.funktor.cluster.depot.api.DepotItemModel
import kotlinx.serialization.SerialName

fun depotPutFileOptions(block: DepotPutFileOptions.Builder.() -> Unit = {}) =
    DepotPutFileOptions.Builder().apply(block).build()

data class DepotPutFileOptions(
    val meta: DepotItemModel.Meta,
    val encryption: Encryption?,
) {
    companion object {
        val default: DepotPutFileOptions = depotPutFileOptions()
    }

    sealed class Encryption {

        abstract val algoName: String

        @SerialName(AesGcmNoPadding.algoName)
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
        private var meta: DepotItemModel.Meta = DepotItemModel.Meta.default

        private var encryption: Encryption? = null

        internal fun build() = DepotPutFileOptions(
            meta = meta,
            encryption = encryption,
        )

        fun meta(meta: DepotItemModel.Meta) {
            this.meta = meta
        }

        fun encryption(encryption: Encryption) {
            this.encryption = encryption
        }
    }
}
