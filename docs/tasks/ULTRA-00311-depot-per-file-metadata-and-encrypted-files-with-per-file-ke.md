# ULTRA-00311: Depot: per-file metadata, and encrypted files with per-file keys

> **Status (2026-11-11): DESIGNED.** The maintainer on 2026-11-11: "I would like to be able to set meta-data for each
> file. On AWS this should work out of the box. For the file-system we would need to store an additional file, maybe
> <original-filename>.meta or similar. Why? ... store personal customer data in aws. We would generate a one-time
> secret, encrypt the file and upload the file, while setting the encryption keys ID on the meta data and storing the
> key in the database." Decisions below are open; nothing is built.

**Security-critical:** yes (keys, personal data; a red-team ticket follows the gate)

## Why this exists

Envelope encryption with crypto-shredding: every sensitive file gets its own data key; the file is stored encrypted,
its metadata names the key (`keyId`), the key lives in the app's database. The bucket alone is worthless; deleting
the key makes the file unreadable everywhere, backups included — a clean "erase this customer's data".

What the depot has today (`funktor/cluster/src/jvmMain/kotlin/depot/`):
- `DepotPutFileOptions` carries `meta: DepotItemModel.Meta` and `encryption: Encryption?` (`AesGcmNoPadding`), but no
  repository applies either: the FS `putFile` has `TODO: apply encryption`, the options are built and dropped.
- `DepotItemModel.Meta` is a fixed record (visibility, mimeType, organisationId/Name, ownerId), not free key/values.
- S3 `getMeta` reads object **tags** (`GetObjectTaggingRequest`); FS `getMeta` returns null.

## Decide before implementing

1. **The metadata model.** Recommendation: free string key/values (`Map<String, String>`), with the known fields kept
   as typed accessors over well-known keys. The keyId, the cipher and its IV/nonce go in under reserved keys.
2. **S3: user metadata or tags.** User metadata (`x-amz-meta-*`) is written with the object, travels with copies,
   2 KB in total, immutable without a copy; tags are mutable, at most 10, and meant for lifecycle and IAM rules.
   Recommendation: user metadata for the file's own facts (keyId, cipher, IV, mime type), tags left to operators.
   Changes today's `getMeta`, which reads tags.
3. **File system: where the metadata lives.**
   - (a) A sidecar `<name>.meta` next to the file: simple, but every listing must hide it, a user file named
     `x.meta` would collide, and moves or deletes must carry both.
   - (b) A parallel tree, `<root>/.meta/<path>.json`: no collision, listings unaffected, the path rules apply as for
     the file.
   Recommendation: (b). Either way: written with the file (file first, then meta, or meta via a temp file and a
   rename), removed with it, never listed, never reachable as a file through the depot API.
4. **Who encrypts.** Recommendation: the depot. `putFile(..., encryption = AesGcm(key))` encrypts with a key the
   caller passes, stores cipher, IV and the caller's `keyId` in the metadata; `getContent(path, keys)` decrypts with
   a key resolver (`keyId -> key`) the app implements against its database. The depot never stores a key. The app
   generates a fresh key per file (`SecureRandom`, 256 bit). Alternative: the app encrypts and only uses the
   metadata; simpler for the depot, but every app re-implements AES-GCM correctly.
5. **Integrity and reading without the key.** AES-GCM authenticates: a tampered or truncated file fails to decrypt,
   never returns garbage. A file whose key is gone answers a distinct "key not available" (shredded), not "not found".
6. **Key wrapping (later?).** Storing raw data keys in the database vs wrapping them with a master key (KMS on AWS).
   Recommendation: start with keys in the database as the maintainer describes, design the resolver so a KMS-wrapped
   key fits later.
7. **Streaming.** Files read and written whole today (`ByteArray`). Large files would need streaming AES-GCM
   (chunked). Recommendation: whole files first, a size note in the KDoc.

## The work (after the decisions)

1. Metadata model + S3 user metadata + FS metadata store; `getMeta`/`putFile` round-trip on both backends.
2. Encryption in `putFile`/`getContent` with the caller's key and a key resolver; the shredded case.
3. E2E on both backends (`MatrixTest2d` where storage is involved; S3 against a local S3 such as MinIO, or a stub at
   the SDK boundary), mutation checks (security and persistence).
4. Docs, CHANGELOG; a red-team ticket (key leakage, metadata tampering, a sidecar/meta-tree path escape, replay of an
   old encrypted object under a new keyId).

## History

- 2026-11-11: created from the maintainer's request; decisions open.
