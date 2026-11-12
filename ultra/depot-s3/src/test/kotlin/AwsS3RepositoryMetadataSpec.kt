package io.peekandpoke.ultra.depot.s3

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.depot.crypto.EncryptingDepotRepository
import io.peekandpoke.ultra.depot.crypto.InMemoryDepotKeyStore
import io.peekandpoke.ultra.depot.domain.DepotContent
import io.peekandpoke.ultra.depot.domain.DepotSubjectId
import io.peekandpoke.ultra.depot.domain.DepotVerification
import io.peekandpoke.ultra.depot.repos.DepotInvalidMetadataException
import software.amazon.awssdk.core.ResponseBytes
import software.amazon.awssdk.services.s3.S3AsyncClient
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectResponse
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.HeadObjectResponse
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.PutObjectResponse
import software.amazon.awssdk.services.s3.model.S3Exception
import java.lang.reflect.Proxy
import java.util.concurrent.CompletableFuture

/**
 * The S3 repository's metadata, against a stub at the SDK boundary that keeps what `putObject` was asked to store and
 * answers `headObject` from it (ULTRA-00311). No real S3 is involved: what S3 itself does with the request is untested.
 */
class AwsS3RepositoryMetadataSpec : StringSpec({

    class Stub {
        val puts = mutableListOf<PutObjectRequest>()

        /** What a GET answers, by key; a key without one fails with [failure], or "no such key". */
        val bodies = mutableMapOf<String, ByteArray>()
        var failure: Exception? = null

        val client = Proxy.newProxyInstance(
            S3AsyncClient::class.java.classLoader,
            arrayOf(S3AsyncClient::class.java),
        ) { _, method, args ->
            when (method.name) {
                "putObject" -> {
                    puts.add(args[0] as PutObjectRequest)
                    CompletableFuture.completedFuture(PutObjectResponse.builder().eTag("etag").build())
                }

                "headObject" -> {
                    val key = (args[0] as HeadObjectRequest).key()
                    when (val put = puts.lastOrNull { it.key() == key }) {
                        null -> CompletableFuture.failedFuture<HeadObjectResponse>(NoSuchKeyException.builder().build())
                        else -> CompletableFuture.completedFuture(HeadObjectResponse.builder().metadata(put.metadata()).build())
                    }
                }

                "getObject" -> {
                    val key = (args[0] as GetObjectRequest).key()
                    when (val body = bodies[key]) {
                        null -> CompletableFuture.failedFuture<Any>(failure ?: NoSuchKeyException.builder().build())
                        else -> CompletableFuture.completedFuture<Any>(
                            ResponseBytes.fromByteArray(GetObjectResponse.builder().build(), body)
                        )
                    }
                }

                else -> throw AssertionError("S3 was called: ${method.name}")
            }
        } as S3AsyncClient
    }

    fun repoOver(stub: Stub) = object : AwsS3Repository<Any>(s3 = stub.client, bucketName = "bucket") {}

    "metadata goes out as user metadata, values percent-encoded, with the subject and the content type" {
        val stub = Stub()

        repoOver(stub).putFile(
            DepotSubjectId.of(Any::class, "customers", "42"),
            "docs/contract.pdf",
            "pdf".toByteArray(),
            mapOf("mime-type" to "application/pdf", "title" to "Vertrag März"),
        )

        val request = stub.puts.single()
        request.bucket() shouldBe "bucket"
        request.key() shouldBe "docs/contract.pdf"
        request.contentType() shouldBe "application/pdf"
        request.metadata() shouldBe mapOf(
            "mime-type" to "application%2Fpdf",
            "title" to "Vertrag%20M%C3%A4rz",
            "depot-subject" to "customers%2F42",
            "depot-sha256" to "c35b21d6ca39aa7cc3b79a705d989f1a6e88b99ab43988d74048799e3db926a3",
        )
    }

    "metadata reads back decoded; no such object reads as null" {
        val stub = Stub()
        val repo = repoOver(stub)
        repo.putFile(DepotSubjectId.of(Any::class, "customers", "42"), "a.txt", "a".toByteArray(), mapOf("title" to "Vertrag März"))

        repo.putFile(DepotSubjectId.system, "plus.txt", "a".toByteArray(), mapOf("phone" to "+49 30 1"))

        repo.getMeta("a.txt")?.minus("depot-sha256") shouldBe mapOf("title" to "Vertrag März", "depot-subject" to "customers/42")
        repo.getMeta("plus.txt")?.minus("depot-sha256") shouldBe mapOf("phone" to "+49 30 1", "depot-subject" to "system")
        repo.getMeta("nothing.txt") shouldBe null
    }

    "content reads as found or not found; another S3 error is thrown, and verify does not call it a missing file" {
        val stub = Stub()
        val repo = repoOver(stub)
        repo.putFile(DepotSubjectId.system, "a.txt", "a".toByteArray())
        stub.bodies["a.txt"] = "a".toByteArray()

        String(repo.getContent("a.txt").bytesOrNull()!!) shouldBe "a"
        repo.getContent("nothing.txt") shouldBe DepotContent.NotFound
        repo.verify("a.txt") shouldBe DepotVerification.Intact

        stub.bodies["a.txt"] = "b".toByteArray()
        repo.verify("a.txt") shouldBe DepotVerification.Damaged

        stub.bodies.clear()
        stub.failure = S3Exception.builder().message("slow down").statusCode(503).build()

        shouldThrow<S3Exception> { repo.getContent("a.txt") }
        shouldThrow<S3Exception> { repo.verify("a.txt") }
    }

    "an encrypted object gets no content type, and its key's id and method as metadata" {
        val stub = Stub()
        val repo = object : EncryptingDepotRepository<Any>(repoOver(stub), InMemoryDepotKeyStore()) {}

        repo.putFile(DepotSubjectId.of(Any::class, "customers", "42"), "a.pdf", "pdf".toByteArray(), mapOf("mime-type" to "application/pdf"))

        val request = stub.puts.single()
        request.contentType() shouldBe null
        request.metadata()["mime-type"] shouldBe "application%2Fpdf"
        request.metadata()["depot-enc"] shouldBe "aes256gcm-v1"
        request.metadata()["depot-subject"] shouldBe "customers%2F42"
        (request.metadata()["depot-key"] != null) shouldBe true
    }

    "two repositories on one bucket are in one place, under any names" {
        val stub = Stub()
        val first = repoOver(stub)
        val renamed = object : AwsS3Repository<Any>(s3 = stub.client, bucketName = "bucket") {
            override val name = "exports"
        }
        val other = object : AwsS3Repository<Any>(s3 = stub.client, bucketName = "other") {}

        first.storageId.overlaps(renamed.storageId) shouldBe true
        first.storageId.overlaps(other.storageId) shouldBe false
    }

    "refused metadata never reaches S3" {
        val stub = Stub()
        val repo = repoOver(stub)

        shouldThrow<DepotInvalidMetadataException> {
            repo.putFile(DepotSubjectId.system, "a.txt", "a".toByteArray(), mapOf("depot-key" to "x"))
        }
        shouldThrow<DepotInvalidMetadataException> {
            repo.putFile(DepotSubjectId.system, "a.txt", "a".toByteArray(), mapOf("note" to "v".repeat(2048)))
        }

        stub.puts.size shouldBe 0
    }

    "metadata another tool wrote is read as it is, without a subject, not an error" {
        val stub = Stub()
        val foreign = mapOf("rate" to "50%", "phone" to "+49 30 1")
        stub.puts.add(PutObjectRequest.builder().bucket("bucket").key("foreign.txt").metadata(foreign).build())

        repoOver(stub).getMeta("foreign.txt") shouldBe foreign
    }
})
