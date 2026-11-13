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

## Review record (the ledger) — step 2

Round 1, 2026-11-11, level 1 (`reviewer-high`), blind; charters 1, 2 and 3. **NOT clean: 3 MAJOR.**

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 1 | MAJOR | `DepotSubjectId.kt`, `DepotRepository.kt:12`, CHANGELOG | The subject check is skipped when the id is built at the call: `T` is inferred from the expected type, also `Nothing` for a by-name repository (domain M1; impl M6, security n1) | maintainer decision | Confirmed: `DepotMetadataSpec` itself calls `putFile(DepotSubjectId("customer-43"))` on a `<Customer>` repository. A kind-carrying factory proposed |
| 1 | MAJOR | `FileSystemRepository.kt:246`, `AwsS3Repository.kt:254` | The stored id carries no kind: customer 123 and employee 123 are one subject, and one key in step 3; a real id "system" equals `system` (domain M2) | maintainer decision | Same proposal: the kind is part of the stored value |
| 1 | MAJOR | `DepotPaths.kt:19`, `FileSystemRepository.kt:105-120,148` | `.meta` is reserved by exact spelling: `.META` (case-insensitive file systems), `.meta.` (Windows), or a link aliasing the root reach the metadata tree and can rewrite another file's subject (security M1; impl M4, domain m8, security m1) | fix | Case-insensitive, trailing dots and spaces trimmed; `resolve` refuses a canonical path at or under `<root>/.meta`; listing filter by the same rule |
| 1 | MINOR | `FileSystemRepository.kt:200,250,273` | Metadata keyed by the path asked for: a link and its target split it (impl M3, security m1) | fix | Keyed by the resolved file's path |
| 1 | MINOR | `FileSystemRepository.kt:232,248-253` | A fixed `.tmp` name and metadata-then-content: concurrent writers mix, a failed overwrite leaves old content under new metadata (impl M1, M2; domain m2; security m3) | fix | Unique temp files; content to a temp file, metadata moved, content moved; a lock per path |
| 1 | MINOR | `FileSystemRepository.kt:220` | `<path>.json` lets a file `x` and a folder `x.json` collide; a user can block a name (domain m3, security m2, impl D1) | fix | The tree mirrors the paths without a suffix (`.meta/<path>`): a data path cannot be file and folder at once. A deviation from decision 3's wording, reported to the maintainer |
| 1 | MINOR | `FileSystemRepository.kt:244-253,267-273` | Orphan metadata not removed; a write to a folder or the root leaves one (impl M5, domain m4, security m6) | fix | `removeFile` always removes the metadata; a write to a folder is refused first |
| 1 | MINOR | `AwsS3Repository.kt:235` | `getMeta` throws on foreign metadata (`50%`) (domain m1, impl M7, security n3) | fix | Lenient decode: the raw value |
| 1 | MINOR | `AwsS3Repository.kt:260` | `mime-type` goes into Content-Type unchecked (security m4) | fix | A `type/subtype` token pattern, checked in the shared rules; KDoc says it becomes the served type |
| 1 | MINOR | `FileSystemRepository.kt:209,211`, `exception.kt:4` | `DepotException` carries the caller's path and is not an `Exception` (security m5, domain m6) | fix | No path in the message; extends `RuntimeException` |
| 1 | MINOR | step 3 shape | Inner repositories refuse `depot-*` keys, so the middleware cannot write its own; `getContent` has no room for "shredded" (domain m5) | step 3 | An opt-in raw write for middlewares, and the read result type, are designed with step 3 |
| 1 | MINOR | `cluster.astro:53` | The docs example no longer compiles (domain m7) | fix | |
| 1 | MINOR | tests | Unpinned: the subject in the size count, `metaFile`'s containment, case variants, a root alias, an untyped repository, orphans, links (security m7, impl) | fix | |
| 1 | MINOR | CHANGELOG | An existing top-level `.meta` becomes unreachable; "never listed" holds for the file system only; `AwsHelper` unnamed (domain m4, NITs) | fix | |
| 1 | — | insights | One metadata file per record, always `system` (domain, impl SURPLUS) | maintainer decision | |
| 1 | NIT | `AwsS3Repository.kt:236`, `DepotMetadataSpec` titles, long names, the browser "table" | | fix / reject | Long names (over 246 bytes) no longer matter without the suffix and with random temp names; the horizontal list stays |

Round-1 fixes, verified (everything except the two subject MAJORs and the insights question, which wait for the
maintainer): depot 65/65, depot-s3 11/11, cluster 137, insights 114, funktor-all 288, demo 52. Mutations, each red:
Q1 the case rule, Q2 the canonical `.meta` check in `resolve`, Q3 the mirror's containment, Q5 the mirror's layout,
Q6 a write to a folder, Q7 orphan removal, Q8 the subject in the size count, Q9 the mime rule, Q10 the per-file lock
(400 rounds of two writers), Q11 the lenient S3 decode. Q4 (a second containment check in `tempFile`) survived: it
was redundant with `metaFile`, which always runs first; removed. Deviations: the mirror is `<root>/.meta/files/<path>`
with temporary files in `.meta/tmp` (no `.json` suffix; decision 3 named `<path>.json`). Known and said in the KDoc:
a reader between the two moves of a write sees the new metadata with the old content; step 3 must not turn that
into a "corrupted" answer. Open in the docs: `cluster.astro`'s depot example, rewritten once the subject's shape is
decided.

The maintainer's decisions on round 1 (2026-11-11), built:

- **The subject's shape** (both subject MAJORs): the maintainer proposed that the app's extension takes the stored
  entity ("putFile expects the customer or `Storable<Customer>` and constructs the subject id that way"), agreed
  as: no public constructor; `DepotSubjectId.of(type: KClass<T>, namespace, id)`, stored as `namespace/id`, the
  `KClass` pinning `T` so it is never inferred from what the repository expects; blank parts, a `/` in the
  namespace and `Nothing::class` refused; `system` has no `/`, so no factory produces it. In `funktor:cluster`:
  `Storable<T>.asDepotSubject()` (`collection/key`) and `DepotRepository<T>.putFile(Storable<T>, ...)`.
  Accepted by the maintainer: the subject is tied to the collection's name ("Renaming should rarely happen as this
  would also be a big problem for references between collections").
- **System files without metadata** (the insights question; built on the proposal, the maintainer's "Yes this is
  fine" answered both questions in one line, reported back as such): the file system repository writes no metadata
  file for them and removes an older one after the content is in place; on both stores a file without a stored
  subject reads as `depot-subject = system`.

Verified after the decisions: depot 68, depot-s3 11, cluster 140, insights 114, funktor-all 288, demo 52. Mutations,
each red: R1 the namespace's `/`, R2 a blank id, R3 `Nothing` as the kind, R4 the namespace in the stored value,
R5 no metadata file for a system-only write, R6 removal of the older metadata, R7 and R8 "no stored subject reads
as system" (file system, S3), R9 the collection as the namespace. Refused by the compiler, each tried: C1 the
constructor, C2 a customer's id on a by-name repository, C3 another kind's id on a typed repository, C4 another
kind's stored entity through the extension. Docs: `cluster.astro`'s example rewritten.

Round 2, 2026-11-11, level 2 (`reviewer-xhigh`), blind; charters 1, 2 and 3. **NOT clean: 3 MAJOR.** Second round
without a clean one: stopped, the maintainer asked.

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 2 | MAJOR | `FileSystemRepository.kt:271,349` | `removeFile("a.txt/x")` deletes the metadata file of `a.txt`: the clean-up loop deletes a file as if it were an empty folder (security M1) | fix | Confirmed red by a test; the loop removes folders only |
| 2 | MAJOR | `FileSystemRepository.kt:356-358` | The locks belong to one repository object; a `dynamic` repository is built per request, so two requests do not take turns (impl M1; security m4, domain m5) | fix | Confirmed red by a test with two objects over one folder; the locks are in the companion. Across processes (a shared volume) still open, KDoc says "in this process" |
| 2 | MAJOR | `FileSystemRepository.kt:209,298`, `AwsS3Repository.kt:237`, `DepotMetadata.kt:25` | "No stored subject reads as `system`" contradicts decision 8 ("always written") and the design ("reads as an empty map"): a lost `.meta` tree or a file dropped in from outside reads as belonging to no person (domain M1) | fix (maintainer: back to decision 8) | Built on an answer that did not name this question; reverted |
| 2 | MINOR | `FileSystemRepository.kt:246` | The content move deletes a folder at its target recursively: a race of `p` and `p/x` deletes an acknowledged file (impl m1, domain m2, security m2) | fix | Orphan removal only in the mirror; a content move onto a folder fails |
| 2 | MINOR | `FileSystemRepository.kt:310-318` | No rollback between the two moves: a failed content move (another mount, read-only folder) leaves old content under new metadata; temp files in `.meta/tmp` are not on a sub-mount's file system (impl m2, domain m1, security m3) | fix | |
| 2 | MINOR | `FileSystemRepository.kt:241` | Files are created 0600 through `createTempFile` (before: by umask); crash leftovers in `.meta/tmp` hold content and are never swept; `.meta/tmp` as a link out of the root is not checked (impl m3, m4, m7; domain m3) | fix | |
| 2 | MINOR | `FileSystemRepository.kt:212`, `DepotMetadata` | FS `getMeta` does not add the subject to a metadata file without one, S3 does; no single function for the subject entry; `getMeta` between `isFile` and `readText` can throw (impl m6, domain m4) | fix | |
| 2 | MINOR | `removeMeta`/`ensureMetaParent` | Pruning empty mirror folders races with a neighbour's write, which then fails (impl m5); the write lock is held for the whole content write (security m5) | fix | |
| 2 | MINOR | `DepotMetadata.kt:42`, `AwsS3Repository.kt:243` | Form-encoding (`+` for a space), not percent-encoding; a foreign `+` reads as a space (domain m6) | fix | |
| 2 | MINOR | `DepotMetadata.kt:55` | A `getMeta` result cannot be passed back to `putFile` (domain m7) | fix | `withoutReserved` |
| 2 | MINOR | `cluster.astro:67,71`, `CHANGELOG.md:332` | The example downloads from another repository than it wrote to; the step-1 entry still names `DepotMeta` (domain m8, m9) | fix | |
| 2 | NIT | tests, surrogates, import order, `metaRoot` computed twice | | fix / reject | see below |

Round-2 fixes so far: depot 70, cluster 140, insights 114, funktor-all 288, demo 52. The two new tests were red
before the fixes (the remove below a file; two repository objects racing).

The maintainer on round 2's MAJOR (2026-11-11): "Yes back to [decision] 8". Built: every write records its
subject, system files too; a file without a metadata file (FS) or without the entry (S3) reads without
`depot-subject`, never as `system`. One function, `DepotMetadata.stored(subject, meta)`, builds what both stores
write (domain m4). Also fixed: the docs example's repository (m8), the step-1 changelog clause (m9). Verified: depot
69, depot-s3 11, cluster 140, insights 114, funktor-all 288, demo 52; mutations red: S1 the subject dropped from
`stored`, S2 "no metadata file reads as system".

The maintainer (2026-11-11): "Fix all of the findings and run round 3". The round-2 MINORs and NITs, fixed:
a failed content move puts the old metadata back (or removes the new one); temp files are written before the lock,
created by the umask (not 0600), under a `.meta/tmp` checked to lie inside the root, and stale ones (over an hour)
swept once per root and process; a move falls back to a copying move where an atomic one is not supported (a
sub-mount); a content move onto a folder is refused; the metadata move is tried again when a neighbour's removal
took the mirror folder; `getMeta` survives a removal between its two looks; S3 values are percent-encoded with
`%20`, a `+` is kept on read; `DepotMetadata.withoutReserved`; ids with unpaired surrogates refused; `system`'s KDoc
no longer says "belongs to no person"; the e2e test asserts `.meta` exists; the listing filter's canonical half
is pinned. Verified: depot 74, depot-s3 11, cluster 140, insights 114, funktor-all 288, demo 52. Mutations red:
U1/U2 the rollback, U3 the sweep, U5 the tmp containment, U6/U7 the encoding, U8 surrogates, U9 the file mode,
U10 `withoutReserved`, U11 the listing filter. U4 (a symlink check before the containment check) survived as
redundant and was removed. Not testable here, said so: the copying fallback (needs a second mount), the retry and
the `getMeta` race (timing), the refusal of a content move onto a folder (a race only). Rejected: caching `metaRoot`
(the root may be re-pointed while the app runs). Open by decision: several processes on one folder do not take
turns (KDoc says so); the content hash proposed to the maintainer is not built.

The content hash (maintainer, 2026-11-11: "could we store a salted hash of the file in the metadata so we could
detect tampered data?"; answered that a hash beside the file finds damage, not tampering, which needs a keyed
hash; the split agreed: "Yes split is ok"). Built in step 2: every write stores `depot-sha256` (SHA-256 of the
content as written, hex), and `DepotRepository.verify(path)` answers `Intact`, `Damaged`, `NoHash` or null. **For
step 3:** a keyed hash (HMAC) for unencrypted repositories with a key from the key store; for encrypted ones
AES-GCM covers it, and what `depot-sha256` is taken over (the ciphertext) is decided there. Verified: depot 75,
depot-s3 11, cluster 140, insights 114, funktor-all 288, demo 52; mutations red: H1 the hash not stored, H2 verify
always intact, H3 no hash read as damaged, H4 S3 hashing other bytes. Both race tests now also assert `Intact`.

Round 3 was started at level 3 and stopped unfinished when the maintainer asked for the mid level ("review at mid
level"); no result was read. It runs at level 2 over the state including the hash.

Round 3, 2026-11-11, level 2 (`reviewer-xhigh`, the maintainer: "review at mid level"), blind; charters 1, 2
and 3. **CLEAN: no CRITICAL, no MAJOR** from any of the three. **The step-2 gate is passed.**

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 3 | MINOR | `FileSystemRepository.kt:274,342` | A failing write of `a`, racing a first write of `a/b`, deletes `a/b`'s fresh metadata (impl 1, security 1, domain 9) | fix | The folder check is repeated under the lock, before the metadata moves |
| 3 | MINOR | `FileSystemRepository.kt:363` | A write below a file throws `IllegalStateException` with the absolute path, not `DepotInvalidPathException`; `getMeta` rethrows a raw `FileNotFoundException` (impl 2, security 5) | fix | Refused as an invalid path; `DepotException` without a path |
| 3 | MINOR | `FileSystemRepository.kt:412` | Two spellings of one new file take different locks on a case-insensitive file system (security 2) | fix | The lock is chosen by the lower-case path |
| 3 | MINOR | `DepotRepository.kt:85-91` | `verify`: null for a failed S3 read; `Damaged` possible during a write (impl 4, 5; domain 1, 2) | fix | Throws `DepotException` when the content cannot be read; the KDoc says to verify again |
| 3 | MINOR | `FileSystemRepository.kt:369` | A failing rollback hides the original exception; the copying fallback cannot keep the promise (impl 6, domain 3) | fix | `addSuppressed`; KDoc and CHANGELOG name the exception |
| 3 | MINOR | `DepotVerification.kt:11`, `DepotRepository.kt:54`, CHANGELOG | "The depot did not write the file" is wrong for files from before this release (impl 9, domain 4, security 6) | fix | |
| 3 | MINOR | `DepotMetadata.kt` | No function to read the subject entry, though the ticket confirmed one (domain 6) | fix | `DepotMetadata.subjectOf(meta)` |
| 3 | MINOR | `depot_subjects.kt:13` | A `Ref` woken from client input names any collection; `asDepotSubject` takes it as it is (security 4) | fix (KDoc) | An app's duty, said in the KDoc |
| 3 | MINOR | `DepotMetadataSpec.kt:319` | The rollback test does nothing when run as root (impl 3) | open | Known; the suite does not run as root here |
| 3 | MINOR | `DepotSubjectId.kt:31` | The namespace is free text: `customers/42` and `customer/42` for one person (domain 5) | step 3 / maintainer | Stored entities go through `asDepotSubject()`; a namespace per kind is a question for step 3, where it costs a key |
| 3 | MINOR | nested roots | A repository whose root contains another's can write that one's `.meta` (security 3) | step 3 | The boot check planned there covers one root inside another |
| 3 | MINOR | caller metadata | Stays readable after shredding (domain 7); `mime-type` with parameters refused (impl 8); a user's own top-level `.meta` is overwritten, not only unreachable (impl 7) | step 3 / maintainer | Put to the maintainer |
| 3 | NIT | KDoc of `DepotSubjectId`, `sha256` wording, test hygiene, `reified` on `asDepotSubject` | | fix / open | |

After round 3: depot 75, depot-s3 11, cluster 140, insights 114, funktor-all 288, demo 52. Mutations red: V1 the
refusal below a file, V2 `subjectOf`. Not pinned by a test: `verify`'s throw (the S3 stub has no `getObject`), the
lower-case lock key (needs a case-insensitive file system), the check under the lock (a race).

For step 3, from the reviewers: `verify` must be overridden by a wrapper that changes content; the additional data
binds the canonical repository-relative path; the crypto entries count towards the 2 KB; items returned by the
inner store bypass the middleware and must be wrapped; one subject per file.

The maintainer on the four questions after round 3 (2026-11-12): (1) "Let's keep metadata plain. They should not
contain personal data": said in the `DepotMetadata` KDoc, the CHANGELOG and the docs; step 3 does not encrypt
caller entries. (2) the namespace stays free ("Yes keep as is"): the KDoc says a stored entity's id comes from
`asDepotSubject()`. (3) "Yes to boot check": step 3's boot check refuses one root inside another. (4) mime types
with parameters allowed ("Ok"): `type/subtype` plus `; name=value` with a token or quoted value, no control
characters; mutation W1 (the parameter tail accepting anything) red.

**Step 3, plan confirmed (the maintainer, 2026-11-12: "Plan is ok. getContent should return a sealed
interface"; and: "Should the encryption method be part of the meta data as well? ... Then old data can also be
decrypted which has an old encryption method").** `depot-enc` names the method (`aes256gcm-v1`: cipher, key
wrapping and what is bound), the reader picks the code by it, the writer uses the configured one, an unknown name
is refused, and the name is bound to the ciphertext against a downgrade. No bulk re-encryption yet. `depot-sha256`
of an encrypting repository is over the stored bytes. Step 3a, built: `getContent` returns the sealed
`DepotContent` (`Found`, `NotFound`, `Shredded`) on the interface, both stores, items and the facade;
`DepotFileContent` is gone; S3 throws errors other than "no such key". Verified: depot 75, depot-s3 12, cluster
140, insights 114, funktor-all 288, demo 52; mutations red: X1 S3 reading every error as not found, X2 FS reading
a missing file as empty content.

**Step 3b, built (2026-11-12).** In `ultra/depot`: `crypto/DepotEncryption.kt` (`DepotEncryption`,
`Aes256GcmV1`, `DepotEncryptionContext` with length-prefixed fields), `crypto/DepotKeyStore.kt` (`DepotKeyStore`,
`DepotSubjectKey`, `InMemoryDepotKeyStore`), `crypto/EncryptingDepotRepository.kt`, `DepotCorruptedException`,
`DepotRepository.putRaw` (`@DepotRawWrite`), `storedPath`, `storage`, `overlaps`, the overlap check in
`DepotFacade`'s constructor. In `funktor/cluster`: `depot/keys/` (`VaultDepotKeyStore`, `DepotSubjectKeyRecord`,
`KarangoDepotKeyRepo`, `MonkoDepotKeyRepo`, `NoDepotKeyStore`), wired in `useKarango()` / `useMonko()`.
Deviations from the design above, each a choice of mine:
- The method's parameters are one entry, `depot-enc-params` (`iv.wrapIv.wrappedKey`), not `depot-iv` and
  `depot-wrapped-key`: what a method needs is the method's business.
- The key record's document key is the SHA-256 of the subject, and a key's id is `<document key>.<uuid>`: one
  record per subject by construction, found by id without a query or an index, and an erased key's id is never
  valid again. The store's interface is `getOrCreate(subject)`, `get(id)`, `delete(subject)` as designed.
- The "boot check" runs when the `DepotFacade` is created (it is created with the first use, per request), not
  at application start.
- A check "the key belongs to the file's subject" was removed: mutation E2 showed the binding already covers it.
- An encrypted S3 object gets no content type.
- Not built: the keyed hash (HMAC) for unencrypted repositories, the second half of the hash split.
Verified: depot 89, depot-s3 12, cluster 154 (the key store on Karango and Monko, 7 cases each), insights 114,
funktor-all 289 (`DepotEncryptionApiSpec`: written, read through the facade, browsed through the API, shredded),
demo 52. Mutations red: E1 system refused, E3-E6 each bound field, E7 internal entries hidden, E8 shredded, E9 a
file without marker, E10 the length prefix, E11/E12 the overlap check and nesting, E13 raw write, E14 listed
items decrypt, E15 the hash over stored bytes, E16 the stored path for links, K1 an erased id, K2 the create
retry, K3 delete. Found by the tests: Karango's exceptions extend `Throwable`, so the create retry caught
nothing at first. Not pinned by a test: the mid-write retry of a read (timing), values withheld from the
profiler for the key repositories. Red team: ULTRA-00312.

## Review record (the ledger) — step 3

Round 1, 2026-11-12, level 1 (`reviewer-high`), blind; charters 1, 2 and 3. **NOT clean: 3 distinct MAJOR.**

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 1 | MAJOR | `VaultDepotKeyStore.kt:62`, `EntityRepository.kt:330` | On Karango `remove` swallows every database error as "nothing removed": a failed `shred` returns `false`, the key stays (all three charters) | fix | After "nothing removed" the record is looked up; still there is a `DepotException`. Pinned with a repository that removes nothing |
| 1 | MAJOR | `EncryptingDepotRepository.kt:31,90` | A method passed to the constructor writes files the reader never looks it up for (impl M2; domain m2, security m2) | fix | The readers are the built-in methods plus the one it writes with |
| 1 | MAJOR | `cluster.astro:82`, KDoc, CHANGELOG | "For good, in backups too" is false for database backups that hold the key (domain M2, impl 5) | fix | Said in docs, KDoc and CHANGELOG |
| 1 | MINOR | `FileSystemRepository.kt:381`, `EncryptingDepotRepository.kt:137` | A crash between the two moves of an overwrite leaves new key material with old content: both versions lost (domain m1, security m3) | fix | The key material is the first 72 bytes of the stored content, not metadata (`depot-enc-params` is gone): for one subject, content and metadata of two writes still fit. Another subject's overwrite still reads as corrupted until rewritten |
| 1 | MINOR | `exception.kt:10` | `DepotCorruptedException` is no `DepotException` (domain m4, impl NIT) | fix | |
| 1 | MINOR | `EncryptingDepotRepository.kt:92` | The subject read from the map, not `subjectOf` (domain m3, security n2) | fix | |
| 1 | MINOR | `FileSystemRepository.kt:216` | A removal between the two looks of `getMeta` reads as "not encrypted", not "not found" (impl 1) | fix | Not pinned (a race) |
| 1 | MINOR | `AwsS3Repository.kt:84,90` | `location` from the overridable `name`: two repositories on one bucket pass the overlap check (security m1) | fix | From the bucket's name |
| 1 | MINOR | `DepotFacade.kt:20` | Duplicate names not refused (security m4) | fix | |
| 1 | MINOR | tests | "A key of its own" only compared ciphertexts; a title overclaimed; no S3 test of the encrypted write (impl 2, 3, 4; domain n1) | fix | |
| 1 | NIT | `DepotSubjectKeyRecord.kt:12` | `toString` prints the key (domain n4, security n1) | fix | |
| 1 | NIT | key id holds the SHA-256 of the subject (domain n3); a `depot-key` naming a missing id reads as shredded (domain n2) | | open | With hashed subject ids; whoever writes the metadata can also delete the file |
| 1 | — | rollback of one file to an older version; caller metadata not bound in an encrypting repository (security, for the maintainer) | | red team | ULTRA-00312 items 18 and 23 |
| 1 | — | Karango's `remove(idOrKey)` should swallow only "document not found" (security) | | maintainer | A change with a wider reach; no ticket created |

Also built in this round, the open points (maintainer, 2026-11-12: "Let's finish the open points then"):
- **The keyed hash:** `SignedDepotRepository<T>(inner, keys)`: files stored as they are, `depot-mac` =
  `hmac-sha256-v1:<hex>` with the subject's key over the method, stored path, subject, key id, the caller's metadata
  and the content, each length-prefixed. `verify` answers `Intact`, `Damaged`, `NoHash`, or the new
  `DepotVerification.Unverifiable` (the key was erased). System files are signed too.
- **Values withheld:** pinned on both backends with a recording profiler.
- **The read retry:** pinned with an inner repository that answers wrong content twice (reads), three times
  (corrupted).

Verified after round 1: depot 98, depot-s3 14, cluster 160, insights 114, funktor-all 289, demo 52. Mutations red:
F1 the look after "nothing removed", F2 the repository's own method as a reader, F3/F4 the withholding driver
(Karango, Monko), F5 the record's printout, F6 no content type for an encrypted object, F7 the location by bucket,
F8 duplicate names, F10a-e each part of the keyed hash (path, metadata, erased key, subject, content), F11 the read
retry, F12 a key per file.

Round 2, 2026-11-12, level 2 (`reviewer-xhigh`), blind; charters 1, 2 and 3. **NOT clean: 1 MAJOR** (domain;
the implementation and security charters graded the same point MINOR and returned CLEAN). Second round without a
clean one: fixes built, the maintainer asked before round 3.

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 2 | MAJOR | `SignedDepotRepository.kt:85,89`, `DepotVerification.kt`, CHANGELOG, docs | Who can write the metadata turns a tampered signed file into `NoHash` (remove `depot-mac`) or `Unverifiable` (point `depot-key` at an unknown id), both documented as harmless (domain M1; impl 4, security S5) | fix | A signed file without `depot-mac` is `Damaged`; KDoc, enum, CHANGELOG and docs say only `Intact` proves a file and what `Unverifiable` can mean. An erased key and a forged id cannot be told apart without a record of erased ids |
| 2 | MINOR | `DepotRepository.kt:86` | `storage` hands the inner repository to any holder of the middleware (domain m1, security S1) | fix | Replaced by `storageId: DepotStorageId` (kind, place, nesting); `overlaps` is gone from the interface |
| 2 | MINOR | `EncryptingDepotRepository.kt:144` | `shred(system)` compiles and erases the key that signs system files; `shred` acts on the whole key store (domain m2, impl 3) | fix | Refused; KDoc and CHANGELOG say "every repository on that key store" |
| 2 | MINOR | `DepotEncryption.kt:72`, `SignedDepotRepository.kt:115` | One subject key for AES-GCM wrapping and for HMAC (security S2; domain: no weakness, cheap hygiene) | fix | `DepotSubjectKey.derive(purpose)` (HMAC-SHA-256 as HKDF expands), a label per use; nothing is live |
| 2 | MINOR | `EncryptingDepotRepository.kt:116`, `SignedDepotRepository.kt:62` | A key is made before path, metadata and size are checked (security S4, impl NIT 4) | fix | Checked first; pinned |
| 2 | MINOR | `EncryptingDepotRepository.kt:33` | A deployment moving from one custom method to another cannot read the earlier files (domain m3) | fix | `alsoReads` |
| 2 | MINOR | `DepotMetadata.kt:71` | A metadata value with an unpaired surrogate is stored as `?`: a signed file never verifies (impl 5) | fix | Refused |
| 2 | MINOR | `DepotFacade.kt:21` | The name check compares raw names, a uri normalises them (security S7) | fix | Compared as a uri spells them |
| 2 | MINOR | `DepotKeyStore.kt:7,31` | "Erasing the key erases the files" is false for signed files (security S3) | fix | KDoc of the key, the store and the signed repository |
| 2 | MINOR | tests | The keyed hash's length prefixes, `listItems(path, limit)` and folders unpinned (impl 1, 2); a stale assertion (NIT) | fix | |
| 2 | MINOR | `DepotEncryption.kt:12` | The repository is not bound: a file copied to the same path of another repository on the same key store decrypts (security S8) | maintainer | Red team item 1 corrected |
| 2 | MINOR | `DepotUri.kt:19` | A uri normalises paths; two files can share one uri (security S6; from before this ticket) | maintainer | Red team item 34; no ticket created |
| 2 | MINOR | key id holds the subject's SHA-256 (domain m4); the metadata's loss loses an encrypted file (domain m5); no tombstone after an erasure (domain m6) | | maintainer | Red team items 39, 40 |
| 2 | NIT | `Shredded` once for a live file rewritten during a re-key (security S9) | | fix (KDoc) | |

Verified after round 2: depot 102, depot-s3 14, cluster 160, insights 114, funktor-all 289, demo 52. Mutations red:
G1 the derived wrap key, G2 a signed file without its hash, G3 `shred(system)`, G4 the checks before a key is made,
G6 the value's text, G7 names as a uri spells them, G8 the lengths in the keyed hash, G9 `alsoReads`, G10 the
S3 storage id, G11 nesting, G12/G13 wrapped items of `listItems(path, limit)` and `getFolder`.

The maintainer on round 2 (2026-11-12): round 3 at mid level ("yes"); a file copied into another repository on
the same key store decrypting there stays ("leave as is"); a tombstone after an erasure ("yes add it"); no key
id in the stored bytes against a lost `.meta` ("no need ... the backups need to be correct instead"); tickets for
Karango's `remove` (ULTRA-00313) and `DepotUri` (ULTRA-00314, "Tackle the normalized DepotUri right away").
Built:
- **Tombstone:** `DepotKeyStore.delete` remembers the erasure; `getOrCreate` then throws
  `DepotSubjectErasedException`; `isErased(id)` is new. In the database the record stays, emptied of key and
  subject id (it is found by the hash of the subject id, and keeps the erased key's id). A subject erased before
  it had a key gets a tombstone too. One subject has one key, ever.
- **Erased or forged, told apart:** with `isErased`, a file naming a key that never existed is corrupted
  (encrypting) or `Damaged` (signed); only an erased key reads as `Shredded` / `Unverifiable`. This closes what
  round 2's MAJOR had left to wording.
- The look-up after Karango's "nothing removed" is gone with it: an erasure is a save now, which throws.
- **ULTRA-00314:** `DepotUri` exact; names a uri cannot carry refused.

Verified: depot 109, depot-s3 14, cluster 160, insights 114, funktor-all 289, demo 52. Mutations red: H1 the
erased subject refused (database), H2 an erased key not handed out, H3 the tombstone emptied, H4 a tombstone for
a subject without a key, H5 `isErased`, H6/H7 erased told from forged (encrypting, signed), H8 the in-memory
store's refusal, H9 the uri's path as it is, H10 the name rule, H11 the facade's name check.

Round 3, 2026-11-12, level 2 (`reviewer-xhigh`), blind; charters 1, 2 and 3. **NOT clean: 1 MAJOR** (domain,
a test gap; implementation and security returned CLEAN). The security reviewer reported having read the round-2
table by mistake; its finding 1 matches the other two reviewers' independent ones.

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 3 | MAJOR | `DepotEncryption.kt`, `DepotKeyStore.kt:18`, `SignedDepotRepository.kt:121` | Nothing pins the stored formats: every test writes and reads with the same code, so a mirrored change (a purpose text, the field order, the derivation, the metadata's order) stays green while every stored file stops reading (domain 1) | fix | `DepotStoredFormatSpec`: bytes made by another implementation (Python `hmac`, `cryptography` AESGCM) for the derivation, a stored `aes256gcm-v1` file (decrypt, and encrypt with given IVs), and a `depot-mac` with two metadata entries out of order. They matched the code at the first run |
| 3 | MINOR | `EncryptingDepotRepository.kt:119`, `SignedDepotRepository.kt:61` | The size check before a key is made leaves out the depot's own entries (all three charters) | fix | Checked with the method's name and a reserve of `DepotKeyStore.MAX_KEY_ID_LENGTH`; the comment no longer claims the store's refusals |
| 3 | MINOR | `VaultDepotKeyStore.kt:66` | The guard "this record's key has that id" unpinned on the database store (impl 2) | fix | |
| 3 | MINOR | `DepotUri.kt:9,18` | `copy()` skips the name rule; the empty path's uri does not read back (impl 3, 4; security B, C) | fix | `@ConsistentCopyVisibility`; `(.*)` |
| 3 | MINOR | `FileSystemRepository.kt:275` | A write of `a` racing a finished write of `a/b` deletes the mirror folder with `a/b`'s metadata (security 2) | fix | Refused when the content path has become a folder, checked right before the mirror's folder would be removed; not pinned (a race) |
| 3 | MINOR | `EncryptingDepotRepository.kt:66` | `verify` says `Intact` for ciphertext replaced together with its hash (security 4) | fix | Decrypts while the key is there |
| 3 | MINOR | erased ids | An erased subject id is burnt for whoever gets it next (security 3); the tombstone keeps a hash of the id: pseudonymous (domain 4) | fix (docs) | KDoc, CHANGELOG, docs |
| 3 | MINOR | `CHANGELOG.md:376` | "Copied elsewhere" is wrong for the decided same-path case (domain 3) | fix | |
| 3 | NIT | the in-memory store's check-then-act; `HEADER_BYTES` public | | fix | |
| 3 | NIT | `SignedDepotRepository` is named for a signature and makes a keyed hash (domain 5); the S3 storage id ignores the endpoint (impl 7, domain 6); `alsoReads` and the custom method are not asked for (domain) | | settled (maintainer, 2026-11-13) | The name stays, the endpoint stays out of the storage id, the parameters stay |

Verified after round 3: depot 115, depot-s3 14, cluster 160, insights 114, funktor-all 289, demo 52. Mutations
red: I1 the wrap purpose, I2 the derivation's counter, I3 the context's field order, I5 the metadata's order in
the keyed hash, I6 its field count, I7 the signing purpose, I8 the size reserve, I9 the decrypting verify, I10
the key-id guard of the database store, I11 the empty path's uri.

The maintainer on round 3 (2026-11-13): "one more round"; `SignedDepotRepository` keeps its name; two S3
endpoints with one bucket name stay refused as one storage; `encryption` and `alsoReads` stay.

Round 4, 2026-11-13, level 2 (`reviewer-xhigh`), blind; charters 1, 2 and 3. **CLEAN: no CRITICAL, no MAJOR**
from any of the three. **The step-3 gate is passed** (with ULTRA-00314).

| Round | Sev | `path:line` | Claim | Disposition | Reason |
|---|---|---|---|---|---|
| 4 | MINOR | `EncryptingDepotRepository.kt:71-83` | `verify`: `NoHash` for a file whose hash entry was removed, `Intact` for a shredded file (by its plain hash alone) and for one removed between the reads, an exception for a missing method (all three charters) | fix | Only a file that fits its hash and decrypts is `Intact`; no hash or no method is `Damaged`; shredded is `Unverifiable`; no file is null |
| 4 | MINOR | `VaultDepotKeyStore.kt:95` | The key record's address (SHA-256 hex of the subject's UTF-8) and the key's stored form unpinned: a change would orphan every key, and an erasure would miss the old one silently (domain 1) | fix | Pinned with a hash computed by `sha256sum` and the base64 of the key, on both backends |
| 4 | MINOR | `DepotStoredFormatSpec.kt:30` | The AES fixture's context was ASCII: the bound fields' encoding unpinned (domain 2); the generator not in the tree (domain 4) | fix | Path and subject not ASCII, bytes made again; the generator in the spec's KDoc |
| 4 | MINOR | `DepotSubjectId.kt:14` | The checks live in `of`, a codec skips them: `system`, blank, no namespace (security 1) | fix | The shape is checked in `init`; KDoc: never read one from a client's input (`system` is a valid value) |
| 4 | MINOR | tests | The signed repository's checks before a key, `DOT_MATCHES_ALL`, the mirror-folder guard unpinned (impl 2, 3, 4) | fix / known | The first two pinned; the guard is a race, the KDoc names the limit (`a` and `a/b` at the same moment) |
| 4 | MINOR | `exception.kt:9` | "Though its key is there" is wrong for a key that never existed (impl 5) | fix | |
| 4 | NIT | the in-memory store removed a key before recording it as erased (security 2); the content hashed once more for the size check (impl) | | fix / reject | The hash's cost is one pass per write |
| 4 | — | caller metadata of an encrypted file not bound; the database's own logs keep a key for a while (domain, for the maintainer) | | red team / known | Items 23, 35 |

Verified after round 4: depot 117, depot-s3 14, cluster 162, insights 114, funktor-all 289, demo 52. Mutations
red: J1 a file without its method, J2 a file without its hash, J3 a shredded file, J4 the bound fields' encoding,
J5 a line break in a uri's path, J6 the signed repository's key-id reserve (it survived at first: the test's size
did not tell the reserve from the keyed hash's own length; tightened), J7 the key record's address.

Probed and clean, step 3 round 1: AES-GCM use (fresh file key, random IVs, 128-bit tag, the bound context on
content and wrap), parameter parsing, an erased key id never valid again, `getContent`'s distinct answers,
`storedPath` through links, refusals (system, raw write, a file without marker), the file-system overlap check,
the profiler withholding, exception messages without paths or keys, the browse API.

Probed and clean (round 2): the subject typing (`of`, `KClass` invariance, `Nothing`, `system` not producible, the
`Storable` extensions on both backends); forging through `meta`; `.meta` under every spelling and alias; header
injection towards S3; error messages; the browse API; the size rule; test expectations are literals.

Probed and clean (round 1): variance; reserved keys cannot be forged through `meta`; size measured as S3 counts it;
JSON and header encoding; `getMeta` null versus empty on both stores; a refused write leaves nothing; browse is
superuser-only and renders text; the S3 stub is faithful for what the tests claim.

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
- 2026-11-11: step 2 done. `DepotSubjectId<out T>`, `DepotRepository<in T>`, `DepotMetadata` (rules, `depot-subject`,
  `mime-type`), the FS `.meta` tree (meta before file, temp file and atomic move, removed with the file, hidden from
  the root listing, `.meta` refused by the shared path check), S3 user metadata by `HeadObject` (tags and `AwsHelper`
  gone). The 2 KB limit applies in every repository, so a file system setup cannot outgrow S3. Writers: insights and
  the demo upload pass `system`. Inspect browser: the map as a list. Insights writes one small metadata file per
  record now (the subject is always recorded).
  Tests: `DepotMetadataSpec` (10), `AwsS3RepositoryMetadataSpec` (3, a capturing stub; covers `getMeta`, carried from
  step 1), `DepotMetaApiSpec` (e2e, 2). Green: depot 54, depot-s3 10, cluster 137, insights 114, funktor-all 288,
  demo 52, inspect 17, codegen 104. Mutations N1-N11, each red: reserved keys, key rule, size, encoded size, listing
  filter, reserved path, metadata removal, the subject entry, S3 encode, S3 decode, S3 validation.
  Not tested: that the compiler refuses a wrong subject type (no compile-test harness in these modules); real S3.
  Deferred to after step 3: the docs page and artifact lines for the two modules. Review gate next.
