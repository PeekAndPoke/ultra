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
