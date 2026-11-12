package io.peekandpoke.ultra.depot.s3

import io.peekandpoke.ultra.datetime.MpInstant
import io.peekandpoke.ultra.datetime.mp
import io.peekandpoke.ultra.depot.domain.DepotContent
import io.peekandpoke.ultra.depot.domain.DepotItem
import io.peekandpoke.ultra.depot.domain.DepotSubjectId
import io.peekandpoke.ultra.depot.repos.DepotMetadata
import io.peekandpoke.ultra.depot.repos.DepotRawWrite
import io.peekandpoke.ultra.depot.repos.DepotRepository
import io.peekandpoke.ultra.depot.repos.DepotStorageId
import io.peekandpoke.ultra.depot.repos.validateDepotPath
import java.net.URLDecoder
import kotlinx.coroutines.future.await
import software.amazon.awssdk.core.async.AsyncRequestBody
import software.amazon.awssdk.core.async.AsyncResponseTransformer
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.CommonPrefix
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectResponse
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.ListObjectsRequest
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.S3Object

/**
 * Depot repository implementation for storing files in AWS S3.
 *
 * The repo needs an [S3AsyncClient] to work with.
 * The repo also works on exactly one bucket specified by [bucketName].
 *
 * How to use the AWS-SDK-2:
 * See https://github.com/awsdocs/aws-doc-sdk-examples/blob/master/javav2/example_code/s3
 */
abstract class AwsS3Repository<in T>(
    private val s3: S3AsyncClient,
    private val bucketName: String,
) : DepotRepository<T> {

    /**
     * Implementation of [DepotItem] for S3 repos.
     */
    data class DepotS3File(
        val s3: S3AsyncClient,
        override val repo: AwsS3Repository<*>,
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
        override suspend fun getContent(): DepotContent = repo.getContent(path)
    }

    data class DepotS3Folder(
        val s3: S3AsyncClient,
        override val repo: AwsS3Repository<*>,
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
    override val location: String get() = "S3 Bucket: $bucketName"

    /** By the bucket, not the name: two repositories on one bucket are in one place under any names. */
    override val storageId: DepotStorageId get() = DepotStorageId(kind = "s3", place = bucketName)

    /** The object's key, checked like every path. */
    override suspend fun storedPath(path: String): String = path.validateDepotPath()

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
        // A path that could leave the bucket's layout is refused, as in every repository (ULTRA-00304)
        path.validateDepotPath()

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
        // A path that could leave the bucket's layout is refused, as in every repository (ULTRA-00304)
        path.validateDepotPath()

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
        // A path that could leave the bucket's layout is refused, as in every repository (ULTRA-00304)
        path.validateDepotPath()

        return DepotS3Folder(
            s3 = s3,
            repo = this,
            obj = CommonPrefix.builder()
                .prefix(path)
                .build()
        )
    }

    /** Reads the object at [path]. An S3 error other than "no such key" is thrown, not read as "not found". */
    override suspend fun getContent(path: String): DepotContent {
        // A path that could leave the bucket's layout is refused, as in every repository (ULTRA-00304)
        path.validateDepotPath()

        if (path.isEmpty()) {
            return DepotContent.NotFound
        }

        val request = GetObjectRequest.builder()
            .bucket(bucketName)
            .key(path)
            .build()

        return try {
            DepotContent.Found(s3.getObject(request, AsyncResponseTransformer.toBytes()).await().asByteArray())
        } catch (_: NoSuchKeyException) {
            DepotContent.NotFound
        }
    }

    /**
     * The metadata of the file at [path], from the object's user metadata (`x-amz-meta-*`); values are stored
     * percent-encoded, as headers are ASCII.
     */
    override suspend fun getMeta(path: String): Map<String, String>? {
        // A path that could leave the bucket's layout is refused, as in every repository (ULTRA-00304)
        path.validateDepotPath()

        if (path.isEmpty()) {
            return null
        }

        val request = HeadObjectRequest.builder()
            .bucket(bucketName)
            .key(path)
            .build()

        return try {
            s3.headObject(request).await().metadata().mapValues { (_, value) -> decode(value) }
        } catch (_: NoSuchKeyException) {
            null
        }
    }

    /**
     * A value as the depot wrote it: `%XX` decoded, a `+` kept. One another tool wrote with a bare `%` is taken as it
     * is; one with a `%XX` of its own is decoded too.
     */
    private fun decode(value: String): String = try {
        URLDecoder.decode(value.replace("+", "%2B"), Charsets.UTF_8)
    } catch (_: IllegalArgumentException) {
        value
    }

    /**
     * Puts a file with the given [path] and [content] into the bucket, its metadata as the object's user metadata.
     * A [DepotMetadata.MIME_TYPE] entry also sets the object's content type.
     */
    @OptIn(DepotRawWrite::class)
    override suspend fun putFile(
        subject: DepotSubjectId<T>,
        path: String,
        content: ByteArray,
        meta: Map<String, String>,
    ): DepotItem = putRaw(path = path, content = content, stored = DepotMetadata.stored(subject, meta, content))

    /** An encrypted file gets no content type: its bytes are not of the type its metadata names. */
    @DepotRawWrite
    override suspend fun putRaw(path: String, content: ByteArray, stored: Map<String, String>): DepotItem {
        // A path that could leave the bucket's layout is refused, as in every repository (ULTRA-00304)
        path.validateDepotPath()

        DepotMetadata.validateRaw(stored)

        val request = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(path)
            .metadata(stored.mapValues { (_, value) -> DepotMetadata.encode(value) })
            .apply { stored[DepotMetadata.MIME_TYPE]?.takeIf { DepotMetadata.ENC !in stored }?.let { contentType(it) } }
            .build()

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
        // A path that could leave the bucket's layout is refused, as in every repository (ULTRA-00304)
        path.validateDepotPath()

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
