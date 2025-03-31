package de.peekandpoke.funktor.cluster.depot.repos.aws

import de.peekandpoke.funktor.cluster.depot.api.DepotItemModel
import de.peekandpoke.funktor.cluster.depot.domain.DepotFileContent
import de.peekandpoke.funktor.cluster.depot.domain.DepotItem
import de.peekandpoke.funktor.cluster.depot.repos.DepotPutFileOptions
import de.peekandpoke.funktor.cluster.depot.repos.DepotRepository
import de.peekandpoke.funktor.cluster.depot.repos.depotPutFileOptions
import de.peekandpoke.ultra.common.datetime.MpInstant
import de.peekandpoke.ultra.common.datetime.mp
import kotlinx.coroutines.future.await
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.core.async.AsyncResponseTransformer
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.CommonPrefix
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectResponse
import software.amazon.awssdk.services.s3.model.GetObjectTaggingRequest
import software.amazon.awssdk.services.s3.model.ListObjectsRequest
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.S3Object

/**
 * Depot repository implementation for storing files in AWS S3.
 *
 * The repo needs an [S3Client] to work with.
 * The repo also works on exactly one bucket specified by [bucketName].
 *
 * How to use the AWS-SDK-2:
 * See https://github.com/awsdocs/aws-doc-sdk-examples/blob/master/javav2/example_code/s3
 */
abstract class AwsS3Repository(
    private val s3: S3AsyncClient,
    private val bucketName: String,
) : DepotRepository, AwsHelper {

    /**
     * Implementation of [DepotFileContent] for S3 repos.
     */
    data class DepotS3FileContent(
        val s3: S3AsyncClient,
        val request: GetObjectRequest,
    ) : DepotFileContent {
        /**
         * Read the contents of the file as [ByteArray]
         */
        override suspend fun getContentBytes(): ByteArray? {
            return try {
                val objectBytes = s3.getObject(request, AsyncResponseTransformer.toBytes()).await()

                objectBytes.asByteArray()
            } catch (e: RuntimeException) {
                e.printStackTrace()
                null
            }
        }
    }

    /**
     * Implementation of [DepotItem] for S3 repos.
     */
    data class DepotS3File(
        val s3: S3AsyncClient,
        override val repo: AwsS3Repository,
        val obj: S3Object,
    ) : DepotItem.File {
        /** Path of the file */
        override val path: String = obj.key()

        /** Name of the file */
        override val name: String by lazy(LazyThreadSafetyMode.NONE) {
            obj.key().split("/").lastOrNull() ?: ""
        }

        /** The size of the object in bytes */
        override val size: Long?
            get() = obj.size()

        /** Last modification of the file */
        override val lastModifiedAt: MpInstant?
            get() = obj.lastModified()?.mp

        /** Get the contents of the file */
        override suspend fun getContent(): DepotFileContent {
            return repo.getContent(path)
        }
    }

    data class DepotS3Folder(
        val s3: S3AsyncClient,
        override val repo: AwsS3Repository,
        val obj: CommonPrefix,
    ) : DepotItem.Folder {
        override val path: String = obj.prefix()

        override val name: String by lazy {
            path.split("/").lastOrNull { it.isNotEmpty() } ?: ""
        }

        override val lastModifiedAt: MpInstant? = null
    }

    /** The name of the repo is always the bucket name */
    override val name: String = bucketName

    /** The repo type */
    override val type = "AWS S3"

    /** The location of the repo */
    override val location: String get() = "S3 Bucket: $name"

    /**
     * Lists all files in the bucket
     */
    override suspend fun listItems(path: String): List<DepotItem> {
        return listItems(path = path, limit = Int.MAX_VALUE)
    }

    /**
     * Lists [limit] newest files in the bucket
     */
    override suspend fun listItems(path: String, limit: Int): List<DepotItem> {

        val listObjects = ListObjectsRequest.builder()
            .bucket(bucketName)
            .prefix(path)
            .delimiter("/")
            .encodingType("url")
            .maxKeys(limit)
            .build()

        val response = s3.listObjects(listObjects).await()

        val folders = response.commonPrefixes()
            .filter { it.prefix() != path }
            .map { folder -> DepotS3Folder(s3 = s3, repo = this, obj = folder) }

        val files = response.contents()
            .filter { it.key() != path }
            .map { DepotS3File(s3 = s3, repo = this, obj = it) }

        return folders.plus(files)
    }

    override suspend fun getItem(path: String): DepotItem? {
        return getFile(path) ?: getFolder(path)
    }

    /**
     * Gets a file by the given [path]
     */
    override suspend fun getFile(path: String): DepotS3File? {

        if (path.isEmpty() || path == "/") {
            return null
        }

        val request = GetObjectRequest
            .builder()
            .bucket(bucketName)
            .key(path)
            .build()

        val response: GetObjectResponse = try {
            s3.getObject(request, AsyncResponseTransformer.toBytes()).await().response()
        } catch (e: NoSuchKeyException) {
            return null
        }

        return DepotS3File(
            s3 = s3,
            repo = this,
            obj = S3Object.builder()
                .key(path)
                .size(response.contentLength())
                .lastModified(response.lastModified())
                .build()
        )
    }

    override suspend fun getFolder(path: String): DepotItem.Folder {
        return DepotS3Folder(
            s3 = s3,
            repo = this,
            obj = CommonPrefix.builder()
                .prefix(path)
                .build()
        )
    }

    /**
     * Gets the [DepotFileContent] of the file with the given [path]
     */
    override suspend fun getContent(path: String): DepotS3FileContent {

        val request = GetObjectRequest.builder()
            .bucket(bucketName)
            .key(path)
            .build()

        return DepotS3FileContent(s3 = s3, request = request)
    }

    /** Get the meta information of the file with the given [path] */
    override suspend fun getMeta(path: String): DepotItemModel.Meta? {

        if (path.isEmpty()) {
            return null
        }

        val request = GetObjectTaggingRequest.builder()
            .bucket(bucketName)
            .key(path)
            .build()

        return try {
            s3.getObjectTagging(request).await().toMeta()
        } catch (e: NoSuchKeyException) {
            null
        }
    }

    /**
     * Puts a file with the given [path] and [content] into the bucket.
     */
    override suspend fun putFile(
        path: String,
        content: ByteArray,
        options: DepotPutFileOptions.Builder.() -> Unit,
    ): DepotItem {

        val builtOptions = depotPutFileOptions(options)

        val request = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(path)
            .add(builtOptions.meta)
            .apply { builtOptions.meta.mimeType?.let { contentType(it) } }
            .build()

        // TODO: apply encryption to [content]
        val response = s3.putObject(request, AsyncRequestBody.fromBytes(content)).await()

        return DepotS3File(
            s3 = s3,
            repo = this,
            obj = S3Object.builder()
                .key(path)
                .eTag(response.eTag())
                .size(content.size.toLong())
                .build()
        )
    }

    /** Remove a file with a certain uri */
    override suspend fun removeFile(path: String): Boolean {

        try {
            val request = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(path)
                .build()

            s3.deleteObject(request).await()

            return true
        } catch (_: Throwable) {
        }

        return false
    }
}
