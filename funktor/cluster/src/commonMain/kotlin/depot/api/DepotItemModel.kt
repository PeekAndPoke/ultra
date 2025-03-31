package de.peekandpoke.funktor.cluster.depot.api

import de.peekandpoke.ultra.common.datetime.MpInstant
import de.peekandpoke.ultra.slumber.Slumber
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class DepotItemModel {
    /**
     * Meta information for an item
     */
    @Serializable
    data class Meta(
        val visibility: Visibility,
        val mimeType: String?,
        val organisationId: String?,
        val organisationName: String?,
        val ownerId: String?,
    ) {
        companion object {
            val default = Meta(
                visibility = Visibility.Public,
                mimeType = null,
                organisationId = null,
                organisationName = null,
                ownerId = null,
            )
        }
    }

    /**
     * Visibility of an item
     */
    enum class Visibility {
        Public,
        Private;

        companion object {
            fun valueOfOrPublic(value: String?) = when (value) {
                null -> Public
                else -> try {
                    valueOf(value)
                } catch (e: Throwable) {
                    Public
                }
            }
        }
    }

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
