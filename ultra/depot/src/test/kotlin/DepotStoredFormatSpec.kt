package io.peekandpoke.ultra.depot

import io.kotest.core.spec.style.StringSpec
import io.kotest.engine.spec.tempdir
import io.kotest.matchers.shouldBe
import io.peekandpoke.ultra.depot.crypto.Aes256GcmV1
import io.peekandpoke.ultra.depot.crypto.DepotEncryptionContext
import io.peekandpoke.ultra.depot.crypto.DepotKeyStore
import io.peekandpoke.ultra.depot.crypto.DepotSubjectKey
import io.peekandpoke.ultra.depot.crypto.SignedDepotRepository
import io.peekandpoke.ultra.depot.domain.DepotSubjectId
import io.peekandpoke.ultra.depot.repos.fs.FileSystemRepository

/**
 * The stored formats `aes256gcm-v1` and `hmac-sha256-v1`, pinned by bytes another implementation made: a file an
 * earlier build wrote must read in every later one (ULTRA-00311). A change that turns one of these red changes the
 * format: it needs a new method name, not new expected bytes.
 *
 * How the bytes were made (Python 3, `cryptography`):
 * ```
 * key = bytes(range(32))
 * derive = lambda p: hmac.new(key, p.encode() + b"\x01", hashlib.sha256).digest()
 * aad = b"".join(struct.pack(">I", len(f.encode())) + f.encode() for f in [method, path, subject, keyId])
 * wrapped = AESGCM(derive("depot aes256gcm-v1 wrap")).encrypt(wrapIv, fileKey, aad)
 * stored = iv + wrapIv + wrapped + AESGCM(fileKey).encrypt(iv, plain, aad)
 * # hmac-sha256-v1, fields = [method, path, subject, keyId] + sorted metadata as key, value, ...
 * h = hmac.new(derive("depot hmac-sha256-v1 sign"), digestmod=hashlib.sha256); h.update(struct.pack(">I", len(fields)))
 * for f in fields + [content]: h.update(struct.pack(">I", len(f))); h.update(f)
 * ```
 */
class DepotStoredFormatSpec : StringSpec({

    class Customer

    fun bytes(from: Int, size: Int) = ByteArray(size) { (from + it).toByte() }

    fun ByteArray.hex() = joinToString("") { "%02x".format(it) }

    fun String.unhex() = chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    val key = DepotSubjectKey(id = "key-1", subject = "customers/42", key = bytes(0, 32))
    // Not ASCII: the bound fields are UTF-8
    val context = DepotEncryptionContext(method = "aes256gcm-v1", path = "Verträge/März.pdf", subject = "kunden/Zoë", keyId = "key-1")
    val plain = "the customer's contract, Zoë"

    val stored = "c8c9cacbcccdcecfd0d1d2d3dcdddedfe0e1e2e3e4e5e6e7e488f7de032f2d606ed321e0b75d4b2cadf7e619b3fc5e54bc0c5118cb3b8e" +
        "5bf94e4a921508453986fe5f9d325500e8182affa53c231606cad433d4228d0209ef2650d0c8d01ab4a74296d1c7a2812f0519d029ce4846e4425ed8da07"

    "a key for one use is HMAC-SHA-256 of the purpose and a 1, as HKDF expands" {
        key.derive("depot aes256gcm-v1 wrap").hex() shouldBe "fb61813ab49b582547a7a884e3fc915f0b7dcf35878d5fd6f9565de344cbb1d4"
    }

    "aes256gcm-v1: a file written elsewhere decrypts" {
        String(Aes256GcmV1.decrypt(stored.unhex(), key, context)) shouldBe plain
    }

    "aes256gcm-v1: the same key, IVs and content give the same stored bytes" {
        val written = Aes256GcmV1.encrypt(
            plain = plain.toByteArray(),
            key = key,
            context = context,
            fileKey = bytes(100, 32),
            iv = bytes(200, 12),
            wrapIv = bytes(220, 12),
        )

        written.hex() shouldBe stored
    }

    "hmac-sha256-v1: the keyed hash of a file, its metadata in any order" {
        val keys = object : DepotKeyStore {
            override suspend fun getOrCreate(subject: String) = key
            override suspend fun get(id: String) = key.takeIf { it.id == id }
            override suspend fun isErased(id: String) = false
            override suspend fun delete(subject: String) = false
        }
        val inner = object : FileSystemRepository<Customer>("files", tempdir().absolutePath) {}
        val repo = object : SignedDepotRepository<Customer>(inner, keys) {}

        repo.putFile(
            DepotSubjectId.of(Customer::class, "customers", "42"),
            "docs/contract.txt",
            "content",
            linkedMapOf("title" to "Vertrag März", "mime-type" to "text/plain"),
        )

        repo.getMeta("docs/contract.txt")!!["depot-mac"] shouldBe
            "hmac-sha256-v1:9ec8a305b02a35cb7e2ec9e9d074aebdcfd7f566245370f4be46603a753cec17"
    }
})
