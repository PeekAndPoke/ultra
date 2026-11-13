# Release notes

One file per release, `<version>.md`, newest first below. A note is the short read for someone who uses the
libraries and wants to know what a release brings and what to change. The complete record of every change is
[`CHANGELOG.md`](../../CHANGELOG.md).

The notes are also on the docs site: <https://peekandpoke.io/release-notes/>.

## Releases

- [0.117.0](0.117.0.md), 2026-10-31: Kraft drag and drop, leave guards and wake lock; Streams `sampled` and one
  animation frame per frame; three.js and pixi.js members.

## How a note is written

Written by the `/release` skill from the `## Unreleased` section of the changelog, in the release commit.

```markdown
---
version: 0.118.0
date: 2026-11-14
summary: One line, under 160 characters: what this release is about.
---

# 0.118.0

Two or three sentences: what the release is about, and who needs to act.

## Before you update

What breaks, each with what to do. Left out when nothing breaks.

## New

## Fixed

## Artifacts

New, renamed or removed Maven artifacts. Left out when none changed.
```

- **For the reader who updates, not for the one who built it.** Each entry says what changed for a user of the
  library and, where code has to change, how. No ticket history, no review rounds.
- **Short.** One or two lines per entry, grouped by library (Kraft, Streams, Funktor, ...). A release with forty
  changelog entries still fits on a page: fixes of things released broken are named, the rest is "smaller fixes,
  see the changelog".
- **Every breaking change is named**, also the small ones, with the ticket number so the changelog entry is found.
- **Facts only.** No superlatives, nothing the changelog does not say.
- The summary line goes into the list above and onto the docs site's overview.
- **Links are absolute** (`https://github.com/PeekAndPoke/ultra/blob/master/CHANGELOG.md`): the same file is read on GitHub and
  rendered on the docs site, where a relative link leads nowhere. The last line of a note links the changelog.
