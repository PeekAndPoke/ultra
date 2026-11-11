# ULTRA-00311: Depot: per-file metadata, and encrypted files with per-file keys

> **Status (2026-11-11): IN PROGRESS.** Plan confirmed by the maintainer 2026-11-11; step 1 (the move) started.
>
> The maintainer on 2026-11-11: "I would like to be able to set meta-data for each file. On AWS this should work out
> of the box. For the file-system we would need to store an additional file ... store personal customer data in aws.
> We would generate a one-time secret, encrypt the file and upload the file, while setting the encryption keys ID on
> the meta data and storing the key in the database."

**Security-critical:** yes (keys, personal data; a red-team ticket follows the gate)

## Why this exists

Personal data in a bucket should be worthless without the app's database, and erasable for good: every file is
stored encrypted, its metadata names the key, the key lives in the database. Deleting the key makes the data
unreadable everywhere, backups included ("erase this customer").

What the depot has today (`funktor/cluster/src/jvmMain/kotlin/depot/`):
- `DepotPutFileOptions` carries a `meta` and an `encryption`, but no repository applies either (the options are
  built and dropped; the FS `putFile` has `TODO: apply encryption`).
- `DepotItemModel.Meta` is a fixed record (visibility, mimeType, organisationId/Name, ownerId), declared in
  `funktor/inspect` commonMain.
- S3 `getMeta` reads object tags; FS `getMeta` returns null.
- Writers in this repo: `InsightsFull.kt:90` and the demo upload (`ClusterShowcaseApi.kt:236`). Nothing else.
- The core needs nothing from funktor: it imports `ultra:common`, `ultra:datetime` and the AWS SDK. Funktor comes in
  only through the API models (`asApiModel`), `DepotApi` and the kontainer wiring.

## Decisions (the maintainer, 2026-11-11)

1. **Metadata: a free `Map<String, String>`.** `DepotItemModel.Meta` and `DepotPutFileOptions` go.
2. **S3: user metadata (`x-amz-meta-*`); tags dropped and not planned** (no reader on the file system; a need such
   as retention gets its own ticket).
3. **File system: a parallel tree, `<root>/.meta/<path>.json`.**
4. **Encryption as middleware per repository**, with ready key stores as database repositories.
5. **A file whose key is gone answers distinctly** ("shredded"), not "not found".
6. **Keys in the database first; master-key wrapping (KMS) later**, behind the key store.
7. **Whole files first** (the depot has no streaming: `putFile(ByteArray)`, `getContentBytes(): ByteArray`).
8. **Every write names its data subject, checked at compile time.** "Subject", not "owner": the person the data is
   about, whose key encrypts it, whose erasure removes it.

   ```kotlin
   @JvmInline
   value class DepotSubjectId<out T>(val value: String) {
       companion object {
           /** Files that belong to no person: insights records, system uploads */
           val system: DepotSubjectId<Nothing> = DepotSubjectId("system")
       }
   }

   interface DepotRepository<in T> {
       suspend fun putFile(subject: DepotSubjectId<T>, path: String, content: ByteArray, meta: Map<String, String> = emptyMap()): DepotItem
       // list, getFile, getFolder, getContent, getMeta, removeFile: no subject
   }

   class InsightsRepository : DepotRepository<Any>       // any subject
   class CustomerDocsRepo : DepotRepository<Customer>    // only DepotSubjectId<Customer>
   ```

   - The subject id is always written into the metadata, encrypted or not: every file records whose data it is.
   - `system` is a `DepotSubjectId<Nothing>` and so passes the compiler for every repository; an encrypting
     repository refuses it at runtime (one line, tested).
   - `DepotFacade` keeps `Lookup` and its methods. `getRepo(name)` gives a `DepotRepository<*>`: readable and
     removable; its `putFile` accepts only `system`. Real writes go through the typed `getRepo(Cls::class)`.

## The design, in detail (from the review)

**Metadata**
- Keys: `[a-z0-9-]+`, at most 64 characters (S3 lowercases header names; one rule for both stores). Keys starting
  with `depot-` are reserved: `depot-subject`, `depot-enc`, `depot-key`, `depot-iv`, `depot-wrapped-key`.
  A caller's map with a reserved key is refused.
- S3: one `x-amz-meta-<key>` per entry, values percent-encoded UTF-8 (headers are ASCII); the whole map checked
  against S3's 2 KB before the request, refused with a clear error.
- FS: `.meta` is a reserved first path segment: refused by the path check, never listed. The JSON is written through
  a temp file and a rename, meta before the file; a meta without its file reads as "not found" and is overwritten or
  removed with the next write or delete. `removeFile` removes both.
- A file without metadata (everything stored before this) reads as an empty map, unencrypted. No migration.
- `getMeta` returns the caller's entries and `depot-subject`; the crypto entries stay internal.
- The inspect depot browser shows the map as a key/value table (`DepotBrowseModel.meta: Map<String, String>?`).

**Encryption (`EncryptingDepotRepository<T>`, wraps one inner repository)**
- AES-256-GCM, a random 96-bit IV per file, whole file.
- **Envelope keys** (to confirm, see below): each file gets its own random data key; the subject's key from the key
  store wraps it (AES-GCM); the wrapped data key, its IV and the subject key's id go into the metadata. One key per
  subject in the database, one deletion erases the subject, yet no key ever encrypts two files.
- The metadata is not secret and not trusted: path, subject id and key id are bound to the ciphertext as AES-GCM
  additional data, so an encrypted object copied under another path or subject fails to decrypt.
- Read: no `depot-enc` → the bytes as they are (only in a repository that is not encrypting; an encrypting one
  refuses a file without it); unknown `depot-enc` version → refused; key gone → `DepotContent.Shredded`
  (a distinct result, not null, not an exception a caller would map to 500); tampered → `DepotCorruptedException`.
- `shred(subject)` deletes the subject's key. `removeFile` deletes file and metadata, never a subject key.
- The inner repository is built by the middleware from its configuration and is never a kontainer service; a boot
  check refuses two repositories on one bucket or one root directory.
- Listing sizes are ciphertext sizes (plain + 16 bytes); said in the KDoc.

**Key store**
- `DepotKeyStore { getOrCreate(subjectId): Key; get(keyId): Key?; delete(subjectId) }`; keys 256 bit from
  `SecureRandom`.
- Implementations as vault repositories for Karango and Monko in `funktor/cluster` (it has both), tested on both
  backends. The stored key record is a credential: withheld from insights like the other credential repositories.
- Master-key wrapping later replaces what the store persists, nothing else.

**Modules** (asked 2026-11-11; to confirm)
- `ultra/depot` (new, JVM): domain, `DepotRepository`, `DepotSubjectId`, paths, metadata, `FileSystemRepository`,
  `DepotFacade`, the encrypting middleware, `DepotKeyStore`. Depends on `ultra:common`, `ultra:datetime`.
- `ultra/depot-s3` (new, JVM): `AwsS3Repository`, so the core does not carry the AWS SDK.
- `funktor/cluster` keeps the thin part: kontainer wiring, `DepotApi` and the API-model mapping, the database key
  stores. Packages move (`io.peekandpoke.ultra.depot`); breaking, nothing is live.

## Steps, each with its own review gate

1. **Move** the core to `ultra/depot` and `ultra/depot-s3`, behaviour unchanged; all existing tests green.
2. **Subject and metadata:** `DepotSubjectId`, the generic `DepotRepository`, the metadata map on both stores,
   `.meta` tree, S3 user metadata, the two writers pass `system`, inspect browser, docs, CHANGELOG.
3. **Encryption:** the middleware, key stores, shredding; red-team ticket.

Tests: e2e through `AppSpec` (depot browse with metadata; an encrypted repository written, read, shredded), the key
stores on both DB backends (`MatrixTest2d`), mutation checks throughout (security and persistence). S3: a capturing
stub at the SDK boundary asserts the request built (metadata headers, sizes); there is no real S3 on this machine,
and the report will say so until one runs (see "To confirm").

## Confirmed (the maintainer, 2026-11-11)

- **Envelope keys:** each file its own data key, wrapped by the subject's key ("ok").
- **Subject ids in clear** in the metadata; "the path for the hashed id should still stay open for the future": the
  metadata carries a format version (`depot-enc`), and the subject entry is read through one function.
- **S3 tests: stubbed only now**; a real endpoint stays untested and is said so.

## History

- 2026-11-11: created from the maintainer's request.
- 2026-11-11: decisions 1-7 taken with the maintainer.
- 2026-11-11: decision 8 (the subject, a generic value class, always required); plan reviewed: metadata key rules,
  `.meta` reserved, write order, envelope keys, additional data, the unencrypted-file rule, module split, steps.
- 2026-11-11: the three open points confirmed; step 1 started.
- 2026-11-11: IN PROGRESS.
- 2026-11-11: step 1 done: `ultra/depot` (core, 44 tests moved with it) and `ultra/depot-s3` (7), packages
  `io.peekandpoke.ultra.depot[.s3]`; `funktor/cluster` keeps `DepotApi`, the kontainer wiring and the API mapping
  (`depot/api/api_models.kt`); a core `DepotMeta` replaces the funktor model in the core until step 2 replaces it
  with the map; the dead `@SerialName` on `DepotPutFileOptions.Encryption` dropped (the class goes in step 2).
  Green: depot 44/44, depot-s3 7/7, cluster 137/137, insights 114/114, funktor-all 286/286, demo 52/52.
  Not done here: a docs page and the `site.ts`/README artifact lines for the new modules (with step 2, when the
  API settles). Review gate (implementation charter; a move without behaviour change) next.
- 2026-11-11: step 1 review gate, round 1 (level 1, charter 1; a move): **clean, 0 CRITICAL, 0 MAJOR.** Fixed: the
  stale build comment in `funktor/cluster`, two KDoc links in depot-s3 (`[DepotMeta]`, `[S3AsyncClient]`), the unused
  coroutines dependency of `ultra/depot`, import order. Carried into step 2: the S3 capturing-stub test must cover
  `getMeta` to the API model end to end (the successful meta path has no test today); `DepotUriModel.protocol`
  (now a duplicate constant, no users) goes; the CHANGELOG names the `DepotPutFileOptions` removal. Rejected: the
  recorded kontainer-graph fixture naming the old `DepotFacade` class (regenerated by its refresh script).
