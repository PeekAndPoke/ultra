# Semantic UI DSL: fix @DslMarker scoping + remove dead code — DONE (2026-06-24)

## Context

Started as a bundle-size question (Kraft + kotlinx/html + Semantic UI DSL). Investigation
established that **bundle size is not driven by the DSL, annotations, or inlining** — it's
the irreducible Kotlin/JS baseline (measured: hello-world = 1,125 KB raw → 146 KB gzip;
adminapp = 2,262 KB → 438 KB gzip; production already Terser-minified). The `inline val`
icons are a size *win* (unused ones emit 0 JS), `visitNoInline` already neutralizes
kotlinx/html's inline-lambda duplication, and `@DslMarker` annotations cost ~0 bytes. So no
bundle work was done.

Two real code-quality issues were fixed instead:

1. **All `@DslMarker` annotations were inert.** `@DslMarker` only controls implicit-receiver
   scoping when applied to the receiver **class/type** (confirmed via Kotlin docs) — on
   member functions/properties it has **no effect**. None of the receiver classes were
   annotated; only their ~2,250 members were. So DSL scope safety had never actually been
   on, despite wrong-scope usage causing real bugs in downstream apps.
2. **`object Fn`** in `internals.kt` (~400 lines of unused two-letter `const val`s) was dead
   code.

User decision: move markers onto the receiver classes, delete the inert ones, enable scope
safety. Breaking a few downstream consumers accepted/desired (few users, no deprecation).

## What shipped

All under `ultra/semanticui/src/commonMain/kotlin/`:

- **`annotations.kt`** — reduced to four markers, each guarded with
  `@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)` so accidental member-application
  is now a compile error. Added new `SemanticEmojiMarker`. Deleted the three never-on-a-class
  markers (`SemanticUiCssMarker`, `SemanticUiTagMarker`, `SemanticUiConditionalMarker`).
- **Class-level markers added:** `@SemanticUiDslMarker` → `SemanticTag`,
  `@SemanticIconMarker` → `SemanticIcon`, `@SemanticEmojiMarker` → `SemanticEmoji`,
  `@SemanticFlagMarker` → `SemanticFlag`.
- **All inert member markers stripped** from `SemanticTag/Icon/Flag/Emoji.kt` and the
  extension entry points in `index.kt`.
- **Generators kept in sync:** `extract_icons.js` / `extract_flags.js` no longer emit the
  marker prefix.
- **`internals.kt`** — `object Fn` deleted (kept `visitNoInline`).

Net: 9 files, +2,227 / −2,698 lines.

## Verification (all passed)

- `:ultra:semanticui:compileKotlinJvm` + `:compileKotlinJs` — BUILD SUCCESSFUL.
- `:ultra:semanticui:jsTest` — green.
- Consumers compiled clean (no in-repo scope violations surfaced):
  `:kraft:semanticui`, `:kraft:examples:hello-world`, `:kraft:examples:fomanticui`,
  `:funktor-demo:adminapp`.

## Out of scope (deliberately not done)

- Bundle-size work — no meaningful win available from DSL changes.
- `@JsName` mangling re-introduction — reverted earlier; Terser already mangles.
