---
name: code-style
description: Use when writing, reviewing, or fixing Kotlin code style. Enforces import rules, naming conventions, and tooling preferences. Also use when someone asks to check code style, fix imports, or review code conventions.
---

## What This Skill Does

Enforces project-wide code style rules for all Kotlin (and related) code. These rules apply to every file written or
modified in this repository. When invoked explicitly via `/code-style`, scans recently changed files for violations and
fixes them.

## Rules

### 1. No fully qualified class names

Never use fully qualified class names inline in Kotlin code. Always add an `import` statement and use the short name.

**Wrong:**

```kotlin
val clock = kotlinx.datetime.Clock.System
val style = kotlinx.html.style { }
io.peekandpoke.kraft.forms.validation.strings.notBlank()
```

**Right:**

```kotlin
import kotlinx.datetime.Clock
import kotlinx.html.style
import io.peekandpoke.kraft.forms.validation.strings.notBlank

val clock = Clock.System
style { }
notBlank()
```

**If there's a name conflict**, use an import alias — never inline FQCN:

```kotlin
import io.peekandpoke.kraft.testing.input as testInput
import kotlinx.html.input
```

### 2. Always use explicit imports

Never use wildcard/star imports. Import each symbol individually.

**Wrong:**

```kotlin
import io.peekandpoke.kraft.testing.*
import kotlinx.html.*
import io.peekandpoke.ultra.html.*
```

**Right:**

```kotlin
import io.peekandpoke.kraft.testing.TestBed
import io.peekandpoke.kraft.testing.click
import io.peekandpoke.kraft.testing.textContent
import kotlinx.html.div
import kotlinx.html.button
import kotlinx.html.id
import io.peekandpoke.ultra.html.onClick
import io.peekandpoke.ultra.html.onInput
```

### 3. Branding

Use "PeekAndPoke" (CamelCase) or "peekandpoke" (lowercase). Never use "peek&poke" with an ampersand.

### 4. Package management

Use `pnpm` for all JavaScript/Node package management. Never use `npm`.

**Wrong:** `npm install`, `npm run build`
**Right:** `pnpm install`, `pnpm run build`

## When invoked explicitly

When the user runs `/code-style` or asks to check code style:

1. Run `git diff --name-only HEAD` to find recently changed `.kt` files
2. If no changes, ask the user which files or directories to check
3. For each file, scan for violations of the rules above
4. Fix all violations found
5. Report what was fixed

## Notes

- These rules apply to ALL Kotlin code in the project — source, tests, build scripts, examples
- The explicit import rule also prevents subtle bugs from name collisions (e.g., `input()` from KQuery vs `input` from
  kotlinx.html)
- When reviewing code written by agents/subagents, check for these patterns before presenting to the user
