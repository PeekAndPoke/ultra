package de.peekandpoke.funktor.cluster.depot.repos.aws

import de.peekandpoke.funktor.cluster.depot.api.DepotItemModel
import de.peekandpoke.funktor.cluster.depot.domain.DepotItem
import software.amazon.awssdk.services.s3.model.GetObjectTaggingResponse
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.Tag
import software.amazon.awssdk.services.s3.model.Tagging

/**
 * Helpers for interacting with AWS S3
 */
interface AwsHelper {

    companion object {
        const val TAG_VISIBILITY = "visibility"
        const val TAG_MIME_TYPE = "mimetype"
        const val TAG_ORGANISATION_ID = "organisation_id"
        const val TAG_ORGANISATION_NAME = "organisation_name"
        const val TAG_OWNER_ID = "owner_id"
    }

    /**
     * Adds [DepotItemModel.Meta] as tags to the request
     */
    fun PutObjectRequest.Builder.add(meta: DepotItemModel.Meta): PutObjectRequest.Builder =
        apply {
            // Add tags
            tagging(meta.toTags())
            // Add content type
            meta.mimeType?.let { contentType(it) }
        }

    /**
     * Converts a [DepotItemModel.Meta] to [Tagging]
     */
    fun DepotItemModel.Meta.toTags(): Tagging = createTags(
        TAG_VISIBILITY to visibility.toString(),
        TAG_MIME_TYPE to mimeType,
        TAG_ORGANISATION_ID to organisationId,
        TAG_ORGANISATION_NAME to organisationName,
        TAG_OWNER_ID to ownerId,
    )

    /**
     * Converts a [GetObjectTaggingResponse] to [DepotItem.Meta]
     */
    fun GetObjectTaggingResponse.toMeta() = DepotItemModel.Meta(
        visibility = DepotItemModel.Visibility.valueOfOrPublic(
            tagSet().firstOrNull { it.key() == TAG_VISIBILITY }?.value()
        ),
        mimeType = tagSet().getValue(TAG_MIME_TYPE),
        organisationId = tagSet().getValue(TAG_ORGANISATION_ID),
        organisationName = tagSet().getValue(TAG_ORGANISATION_NAME),
        ownerId = tagSet().getValue(TAG_OWNER_ID),
    )

    /**
     * Extracts the tag value for the given [key] or null if the key is not found.
     */
    fun List<Tag>.getValue(key: String): String? {
        return firstOrNull { it.key() == key }?.value()
    }

    /**
     * Helper function for creating a [Tagging]
     */
    fun createTags(vararg kv: Pair<String, String?>): Tagging =
        Tagging.builder()
            .tagSet(
                kv.filter { (_, v) -> v != null }
                    .map { (k, v) -> Tag.builder().key(k).value(v).build() }
            )
            .build()
}
