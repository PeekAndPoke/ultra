package io.peekandpoke.funktor

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldNotContain
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.peekandpoke.funktor.cluster.FunktorClusterApiFeature
import io.peekandpoke.funktor.cluster.depot.api.DepotApi
import io.peekandpoke.funktor.cluster.depot.keys.VaultDepotKeyStore
import io.peekandpoke.funktor.inspect.cluster.depot.api.DepotBrowseModel
import io.peekandpoke.ultra.depot.DepotFacade
import io.peekandpoke.ultra.depot.crypto.DepotKeyStore
import io.peekandpoke.ultra.depot.domain.DepotContent
import io.peekandpoke.ultra.depot.domain.DepotSubjectId
import io.peekandpoke.ultra.depot.domain.DepotUri
import io.peekandpoke.ultra.depot.domain.DepotVerification
import java.io.File
import java.util.UUID

/**
 * An encrypting depot repository in the running app, its keys in the database (ULTRA-00311): written, read through
 * the depot, browsed through the API, shredded.
 */
class DepotEncryptionApiSpec : FunktorApiSpec() {

    private val api by service(FunktorClusterApiFeature::class)
    private val repository by service(EncryptedTestFiles::class)
    private val depot by service(DepotFacade::class)
    private val keys by service(DepotKeyStore::class)

    private val secret = "the customer's very private contract"

    init {
        afterSpec {
            File(encryptedDepotDir).listFiles()?.forEach { it.deleteRecursively() }
        }

        api.depot.browse { route ->

            "an encrypted file is written, read, browsed without its key material, and shredded" {
                val anna = DepotSubjectId.of(TestCustomer::class, "customers", UUID.randomUUID().toString())
                val uri = DepotUri("encrypted", "enc-e2e/contract.txt")

                (keys is VaultDepotKeyStore) shouldBe true

                repository.putFile(subject = anna, path = uri.path, content = secret, meta = mapOf("mime-type" to "text/plain"))

                // On disk: not the content
                String(File(encryptedDepotDir, uri.path).readBytes(), Charsets.ISO_8859_1) shouldNotContain "private"
                // Through the depot, by name: decrypted
                String(depot.getContent(uri).bytesOrNull()!!) shouldBe secret

                apiApp {
                    authenticate(superUserToken) {
                        route(DepotApi.BrowseParam(repo = "encrypted", path = uri.path)) {
                            status shouldBe HttpStatusCode.OK
                            apiResponseData<DepotBrowseModel>()?.meta?.keys shouldBe
                                setOf("mime-type", "depot-subject", "depot-sha256", "depot-enc")
                            bodyAsText() shouldNotContain "depot-key"
                            bodyAsText() shouldNotContain "depot-enc-params"
                        }
                    }
                }

                repository.shred(anna) shouldBe true

                depot.getContent(uri) shouldBe DepotContent.Shredded
                repository.verify(uri.path) shouldBe DepotVerification.Intact
            }
        }
    }
}
