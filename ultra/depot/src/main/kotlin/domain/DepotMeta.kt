package io.peekandpoke.ultra.depot.domain

/** Meta information of a depot item. */
data class DepotMeta(
    val visibility: Visibility,
    val mimeType: String?,
    val organisationId: String?,
    val organisationName: String?,
    val ownerId: String?,
) {
    companion object {
        val default = DepotMeta(
            visibility = Visibility.Public,
            mimeType = null,
            organisationId = null,
            organisationName = null,
            ownerId = null,
        )
    }

    /** Visibility of an item */
    enum class Visibility {
        Public,
        Private;

        companion object {
            fun valueOfOrPublic(value: String?) = when (value) {
                null -> Public
                else -> try {
                    valueOf(value)
                } catch (_: Throwable) {
                    Public
                }
            }
        }
    }
}
