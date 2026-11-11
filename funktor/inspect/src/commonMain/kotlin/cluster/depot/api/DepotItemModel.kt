package io.peekandpoke.funktor.inspect.cluster.depot.api

import io.peekandpoke.ultra.common.slumber.Slumber
import io.peekandpoke.ultra.datetime.MpInstant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class DepotItemModel {
    @Serializable
    @SerialName("folder")
    data class Folder(
        override val path: String,
        override val parentPath: String,
        override val name: String,
        override val lastModifiedAt: MpInstant?,
        override val depotUri: DepotUriModel,
    ) : DepotItemModel() {
        override val size: Long? get() = null
    }

    @Serializable
    @SerialName("file")
    data class File(
        override val path: String,
        override val parentPath: String,
        override val name: String,
        override val size: Long?,
        override val lastModifiedAt: MpInstant?,
        override val depotUri: DepotUriModel,
    ) : DepotItemModel()

    @Slumber.Field
    abstract val path: String

    @Slumber.Field
    abstract val parentPath: String

    @Slumber.Field
    abstract val name: String

    /** Size in bytes */
    @Slumber.Field
    abstract val size: Long?

    /** Last modified */
    @Slumber.Field
    abstract val lastModifiedAt: MpInstant?

    @Slumber.Field
    abstract val depotUri: DepotUriModel
}
