package io.peekandpoke.ultra.depot.s3

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.peekandpoke.ultra.depot.domain.DepotSubjectId
import io.peekandpoke.ultra.depot.repos.DepotInvalidPathException
import java.lang.reflect.Proxy
import software.amazon.awssdk.services.s3.S3AsyncClient

/** Every S3 repository method refuses a path that could leave the repository before calling S3 (ULTRA-00304). */
class AwsS3RepositoryPathSpec : StringSpec({

    // A client that fails on any call: a refused path must never reach it
    val unreachable = Proxy.newProxyInstance(
        S3AsyncClient::class.java.classLoader,
        arrayOf(S3AsyncClient::class.java),
    ) { _, method, _ -> throw AssertionError("S3 was called: ${method.name}") } as S3AsyncClient

    val repo = object : AwsS3Repository<Any>(s3 = unreachable, bucketName = "bucket") {}

    val bad = "a/../b"

    "listItems refuses" { shouldThrow<DepotInvalidPathException> { repo.listItems(bad, 10) } }
    "getFile refuses" { shouldThrow<DepotInvalidPathException> { repo.getFile(bad) } }
    "getFolder refuses" { shouldThrow<DepotInvalidPathException> { repo.getFolder(bad) } }
    "getContent refuses" { shouldThrow<DepotInvalidPathException> { repo.getContent(bad) } }
    "getMeta refuses" { shouldThrow<DepotInvalidPathException> { repo.getMeta(bad) } }
    "putFile refuses" { shouldThrow<DepotInvalidPathException> { repo.putFile(DepotSubjectId.system, bad, ByteArray(0)) } }
    "removeFile refuses" { shouldThrow<DepotInvalidPathException> { repo.removeFile(bad) } }
})
