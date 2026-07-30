# Reference only — NOT compiled

These are the original kotlinx.html / SemanticUI renderers for insights, kept verbatim as the spec for
the Vue rewrite: *what did each tab actually show, and how was it laid out.*

**Nothing here is on the compile path.** Gradle compiles `src/jvmMain/kotlin` for this module; this
directory is just files on disk. The code references types that no longer exist (`InsightsGuiTemplate`,
`ultra:semanticui`, `funktor:staticweb`) and is not expected to compile.

## Why it exists

The live collectors were stripped of `renderBar` / `renderDetails` so their `Data` classes could become
plain DTOs — the API serves them, and Vue renders them. The rendering was ~1,062 lines across ten
collectors plus ~527 lines of templates; keeping it commented out inside the live files would have buried
`KontainerCollector`'s 43 lines of data under 423 lines of comments, in exactly the files being reshaped
for Slumber compatibility.

## When to delete

Per collector, as its Vue tab ships: when `KontainerTab.vue` exists, delete
`collectors/KontainerCollector.kt` here. When every tab is done, delete this directory.

The live collectors carry a `// VUE-REF:` pointer to their counterpart here, so `grep -rn "VUE-REF"`
lists what is still outstanding.

## Layout

| Path | Was |
|---|---|
| `collectors/*.kt` | the original collector, data + rendering together |
| `gui/*.kt` | templates, web-resource groups, the ktor routes and the renderer |
| `InsightsCollectorData.kt` | the interface with its `menu` / `content` / `inlineScript` render helpers |
| `assets/*` | the JS/CSS the bar and details pages loaded |

Design context: `.claude/tasks/20260730-frontend-sdk-vue-contributors.md` → "Insights specifics".
