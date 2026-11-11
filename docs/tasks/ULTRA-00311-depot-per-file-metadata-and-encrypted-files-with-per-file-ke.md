# ULTRA-00311: Depot: per-file metadata, and encrypted files with per-file keys

> **Status (2026-11-11): DESIGNED.** The maintainer on 2026-11-11: "I would like to be able to set meta-data for each
> file. On AWS this should work out of the box. For the file-system we would need to store an additional file, maybe
> <original-filename>.meta or similar. Why? ... store personal customer data in aws. We would generate a one-time
> secret, encrypt the file and upload the file, while setting the encryption keys ID on the meta data and storing the
> key in the database." Decisions 1-7 taken 2026-11-11 (below); one open: the key scope. Nothing is built.

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

## Decisions (the maintainer, 2026-11-11)

1. **Metadata: a free `Map<String, String>`, stored as JSON.** The known fields (mime type, owner, ...) become typed
   accessors over well-known keys; keyId, cipher and IV go under reserved keys.
2. **S3: user metadata (`x-amz-meta-*`), tags dropped** (today's `getMeta` reads tags). Real tags are not planned:
   they serve operators (lifecycle, IAM, cost) and have no reader on the file system; a concrete need such as
   retention gets its own ticket.
3. **File system: a parallel tree, `<root>/.meta/<path>.json`.** Written with the file, removed with it, never listed,
   never reachable as a file through the depot API; the path rules apply to it as to the file.
4. **Encryption as middleware per repository**, not per call: an `EncryptingDepotRepository` wraps any repository and
   is registered as that repository; callers do not change.
   - write: a fresh key, AES-GCM, the key through a key store, keyId + cipher + IV into the metadata;
   - read: the key by keyId, decrypt;
   - delete: the file and its key; `shred(path)` deletes only the key.
   - A `DepotKeyStore` interface, with ready implementations as database repositories (Karango and Monko, both
     backends). Other middlewares (compression) compose the same way.
5. **A file whose key is gone answers distinctly** ("shredded"), not "not found". AES-GCM authenticates: a tampered
   or truncated file fails, never returns garbage.
6. **Keys in the database first; master-key wrapping (KMS) later**, through the key store, nothing else changes.
7. **Whole files first.** The depot has no streaming today (`putFile(ByteArray)`, `getContentBytes(): ByteArray`).

## Open

- **Key scope:** one key per file (the maintainer's "one-time secret"; erasing a customer means deleting the keys
  of all their files) or one per data subject (a `subjectId` in the metadata picks the key; erasing a customer is
  one key deletion, also for files the app lost track of), or both, per repository.

## The work (after the decisions)

1. Metadata model + S3 user metadata + FS metadata store; `getMeta`/`putFile` round-trip on both backends.
2. Encryption in `putFile`/`getContent` with the caller's key and a key resolver; the shredded case.
3. E2E on both backends (`MatrixTest2d` where storage is involved; S3 against a local S3 such as MinIO, or a stub at
   the SDK boundary), mutation checks (security and persistence).
4. Docs, CHANGELOG; a red-team ticket (key leakage, metadata tampering, a sidecar/meta-tree path escape, replay of an
   old encrypted object under a new keyId).

## History

- 2026-11-11: created from the maintainer's request; decisions open.
- 2026-11-11: decisions 1-7 taken with the maintainer; the key scope open.
