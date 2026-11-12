# ULTRA-00312: Red team: depot encryption and shredding

> **Status (2026-11-12): DESIGNED.**

## Spec

Concrete attack scenarios against the encrypting depot repository (ULTRA-00311, step 3). Collected here, never
executed in normal feature work.

- [ ] 1. Copy an encrypted file and its metadata to another path, and to another repository of the same subject
      kind: it must not decrypt (path binding).
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
- [ ] 8. Shred racing a write of the same subject: is a file written during the erase readable afterwards?
      (A new key is created; the old one is gone. Decide whether that is acceptable.)
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
- [ ] 17. Shred while the database fails, on Karango (`remove` swallows every error): must throw, never `false`.
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
- [ ] 26. `InMemoryDepotKeyStore` used in production: every file reads as shredded after a restart.
- [ ] 27. A caller rewriting a file for alternating subjects to exhaust readers' retries.
- [ ] 28. Slumber's `AwakerException` prints its input value: a malformed key record echoed into logs.
- [ ] 29. Signed repository: forge a `depot-mac` by reusing one of another file, path or subject; strip the
      `depot-mac` entry so `verify` answers `NoHash` (a caller that treats `NoHash` as fine).

## History

- 2026-11-12: created with ULTRA-00311 step 3.
