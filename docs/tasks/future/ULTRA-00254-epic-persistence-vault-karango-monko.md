# ULTRA-00254: Epic: Persistence (vault, Karango, Monko)

> **Status (2026-10-27): NOT SCHEDULED.** An epic: its progress is its children's (`ticket.py list --epic ULTRA-00254`). The maintainer (2026-10-27): "go through all open tasks and group them into epics".

## Why this exists

Storage: `ultra/vault` and its two backends, Karango (ArangoDB) and Monko (MongoDB).

## What belongs here

The vault scan and its findings, hooks (re-entrancy, consistency checks, mutation tracking), soft-delete unique indexes, id handling and the query printers.

New tickets in this area: `ticket.py new "Title" --part-of ULTRA-00254`.

## History

- 2026-10-27: created; 9 open tickets filed under it.
