# ULTRA-00312: Red team: depot encryption and shredding

> **Status (2026-11-12): DESIGNED.**

## Spec

Concrete attack scenarios against the encrypting depot repository (ULTRA-00311, step 3). Collected here, never
executed in normal feature work.

- [ ] 1. Copy an encrypted file and its metadata to another path: it must not decrypt (path binding). To the
      same relative path in another repository on the same key store: **it decrypts** (the repository is not
      bound; needs write access to the storage). Decide whether a repository identity must be bound.
- [ ] 2. Relabel a file to another subject (metadata edited), with and without that subject's key id: must not
      decrypt, and shredding the original subject must still make it unreadable.
- [ ] 3. Downgrade: rewrite `depot-enc` to another known method once a second method exists; must not decrypt.
- [ ] 4. Swap `depot-enc-params` (the wrapped file key) between two files of one subject, and of two subjects.
- [ ] 5. Reach the inner repository: inject it from the kontainer, register a second repository on the same
      folder or bucket, or one inside the other (the facade's overlap check); a symlinked root; two S3
      repositories on one bucket under different names.
- [ ] 6. Write around the encryption: `putRaw` on the encrypting repository, a `system` write, a file dropped
      into the folder; reading a file without `depot-enc` must be refused, never handed out as it is.
- [ ] 7. After `shred`: every way to the content (items from listings, `DepotFacade.getContent`, the browse
      API, `verify`, a stale `DepotSubjectKey` held in memory by a running request, a cache in front).
- [ ] 8. Shred racing a write of the same subject: a write after the erase is refused; one racing it lands
      under the erased key or fails. Confirm no key is ever made for an erased subject.
- [ ] 9. The key store: read the key collection through insights (vault tab values withheld?), logs, the
      database's own slow-query log, backups; a database dump restores erased keys.
- [ ] 10. The key record's document key is the SHA-256 of the subject id: enumerate subjects with known ids.
- [ ] 11. Two subjects whose ids collide by spelling (`customers/42` by hand and from the stored entity); a
      subject id chosen by a client through an unchecked `Ref`.
- [ ] 12. Metadata is plain: titles and file names that name a person survive a shred.
- [ ] 13. Nonce reuse: the per-file key is random and used once; confirm no code path encrypts twice with one
      file key, and that the wrap IV is random per wrap under the long-lived subject key (2^32 wraps bound).
- [ ] 14. Large files: the whole file is in memory twice; a denial of service by upload size.
- [ ] 15. The mid-write window: a reader between the metadata move and the content move; three attempts,
      then "corrupted". Force it and check nothing but a retry or an error is ever returned.
- [ ] 16. `.meta/tmp` leftovers of a crashed write hold ciphertext; confirm never plaintext.
- [ ] 17. Shred while the database fails: an erasure is a save or an insert, both throw. Must never be `false`.
- [ ] 18. Rollback: restore an older version of one file, content and metadata, at the same path. It decrypts
      and verifies; decide whether a version counter must be bound.
- [ ] 19. A crash between the metadata move and the content move of an encrypted file, same subject and another
      subject.
- [ ] 20. An encryption method that is not built in; a repository without it meeting such a file.
- [ ] 21. S3 repositories on one bucket under overridden names; duplicate repository names in the facade.
- [ ] 22. The overlap check never running: repositories injected directly, no `DepotFacade` ever created.
- [ ] 23. The unbound caller metadata of an encrypted file (`mime-type`) changed on disk and served with that
      type (a signed repository binds it, an encrypting one does not).
- [ ] 24. A client `Ref` naming the key collection (`system_depot_keys/<hash>`), resolved through `RefCodec`.
- [ ] 25. Subject ids differing only in Unicode normalisation (NFC and NFD): two subjects, a shred that misses one.
- [ ] 26. `InMemoryDepotKeyStore` used in production: every file reads as corrupted after a restart.
- [ ] 27. A caller rewriting a file for alternating subjects to exhaust readers' retries.
- [ ] 28. Slumber's `AwakerException` prints its input value: a malformed key record echoed into logs.
- [ ] 29. Signed repository: forge a `depot-mac` by reusing one of another file, path or subject; strip the
      `depot-mac` entry so `verify` answers `NoHash` (a caller that treats `NoHash` as fine).
- [ ] 30. One subject key, two uses: the wrap key and the signing key are derived (`DepotSubjectKey.derive`);
      confirm no code path uses the subject key itself.
- [ ] 31. `depot-key` pointed at an unknown id is `Damaged` / corrupted; at another, erased subject's key id it
      is `Unverifiable` / shredded. A caller that skips those as "erased".
- [ ] 32. Shredding a subject leaves its signed files readable.
- [ ] 33. Key records left by refused writes with client-chosen subjects (checked before the key is made:
      path, metadata, size); a flood of valid first writes for invented subjects.
- [ ] 34. `DepotUri` normalises paths (backslash, surrounding spaces, doubled slashes) and repository names: an
      item's uri names another file or repository. An app that authorises downloads by a stored uri.
- [ ] 35. Erasure lag inside the database: Mongo oplog, delayed secondaries, Arango WAL, unreclaimed pages.
- [ ] 36. Overlap check: bind mounts, a root made a link after the facade was built, case-insensitive file
      systems.
- [ ] 37. `Shredded` reported once for a live file rewritten during a re-key.
- [ ] 38. Key bytes stay in the heap, unzeroed: heap and core dumps.
- [ ] 39. A write for an erased subject is refused (a tombstone in the key store): try to get a key again, by
      another spelling of the subject, by a race of `shred` and a first write, by removing the tombstone.
- [ ] 40. A backup that skips `.meta` (a dot-folder): encrypted files without method, subject and key id.
- [ ] 41. Subject-id reuse after erasure: register, erase, and the id is burnt for whoever gets it next. A
      shred triggered through an unchecked `Ref` is irreversible.
- [ ] 42. The key collection lost (renamed `depotKeysRepoName`, a fresh database, a restore without it, a dev
      `removeAll`): every file reads as corrupted, every tombstone is gone, erased subjects get keys again. A
      restore of an older backup brings back keys and drops tombstones: erasures must be applied again.
- [ ] 43. Concurrent writes of `a` and `a/b` on the file system: the finished write's metadata.
- [ ] 44. Metadata at the size boundary: a key made for a write that is then refused.
- [ ] 45. The stored path on file systems that fold case or Unicode form: a rewrite under another spelling.
- [ ] 46. The per-request entity cache keeps the key record after a shred in the same request.
- [ ] 47. `<sha256(subject)>.none` is computable from a subject id: a forged file naming it reads as erased.
- [ ] 48. A shredded file's listed size gives its content's length (88 bytes more).
- [ ] 49. A `DepotUri` made past the name rule (deserialisation).
- [ ] 50. A custom `VaultDepotKeyStore.Repo` whose `insert` overwrites: the tombstone and "one key, ever" rest
      on insert failing.
- [ ] 51. A `DepotSubjectId` woken by a codec past `of`: `system` in a typed repository (the shape is checked
      on every construction, `system` passes). An app that takes one in a request body.
- [ ] 52. One file-system file under many uris: `/a`, `a//b`, `a/` all read `a`, and `DepotUri` keeps them
      apart. An app that keys ownership or a deny-list by uri or path prefix.
- [ ] 53. Erasing predictable future subject ids (sequential keys) through an unchecked `Ref` before they are
      issued: the tombstone burns them.
- [ ] 54. `verify` of an encrypting repository on a shredded file: `Unverifiable`, never `Intact`. Forged bytes,
      a fitting hash and an erased key's id (see 47).

## History

- 2026-11-12: created with ULTRA-00311 step 3.
