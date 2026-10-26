# ULTRA-00244: Epic: Kraft testing (TestBed, KQuery and friends)

> **Status (2026-10-26): NOT SCHEDULED.** An epic: its progress is its children's (`ticket.py list --epic ULTRA-00244`). The maintainer (2026-10-26): "create an 'epic' Kraft testing and file incoming wishes under it".

## Why this exists

`kraft/testing` (TestBed, KQuery, `eventually`) is what Kraft apps write their browser tests with, and the projects
that use it (Klang, Spross, ultra's own specs) keep finding what gets in the way. Their wishes land here, one child
ticket each.

## How wishes are filed

- One child ticket per wish or per coherent batch: `ticket.py new "Kraft testing: ..." --status IDEA --part-of
  ULTRA-00244`, with who sent it, the project's ticket and commit, and the wish checked against the code.
- Releases are rare (Maven Central limits, release skill): a child that is picked up may grow in a workshop module
  first.

## History

- 2026-10-26: created; ULTRA-00243 (Klang's TestBed/Router wishes) filed under it, ULTRA-00240 (TestBed unmounts,
  failing awaits, KQuery members; done) linked as its first child.
