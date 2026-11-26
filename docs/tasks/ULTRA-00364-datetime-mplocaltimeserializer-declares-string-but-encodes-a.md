# ULTRA-00364: Datetime: MpLocalTimeSerializer declares STRING but encodes a Long

> **Status (2026-11-26): NOT STARTED.** 

## Why this exists

Found by the ULTRA-00343 planner (2026-11-26): `MpLocalTimeSerializer` declares `PrimitiveKind.STRING` but encodes the
time as a Long, milliseconds of the day (`ultra/datetime/src/commonMain/kotlin/MpLocalTimeSerializer.kt:12-17`).
Plain JSON works; formats or tools that trust the declared kind (schema generation, some non-JSON formats) get it
wrong. The maintainer: a separate ticket.

## The work

- Declare `PrimitiveKind.LONG`; the wire format stays unchanged.
- A test that serializes and deserializes a value and checks the descriptor's kind.
- Check the other datetime serializers for the same mismatch.

## Acceptance

- [ ] Descriptor kind matches the encoding for every datetime serializer; wire format unchanged (pinned by a test).

## History

- 2026-11-26: created.
