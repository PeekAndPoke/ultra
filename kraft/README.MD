# Kraft — Build SPAs in Pure Kotlin

A component-based framework for building single-page applications.
Written in Kotlin, with no JavaScript and no template engines.

Kraft compiles to JavaScript and uses [Preact](https://preactjs.com) (a 3KB React alternative) as its rendering engine —
but you never touch Preact directly.

## What you get

- **Components** — Class-based and functional, with props, state, and lifecycle hooks
- **Reactive State** — `var x by value(0)` — change the value, the UI updates
- **Routing** — Type-safe routes with params, middleware, and nested layouts
- **Forms & Validation** — Two-way binding, field validation, draft/commit pattern
- **SemanticUI DSL** — `ui.blue.button { +"Click" }` — a fluent API for FomanticUI
- **14 Addons** — ChartJS, PDF viewer, syntax highlighting, signature pad, and more

## Quick example

```kotlin
class Counter(ctx: Ctx<Props>) : Component<Counter.Props>(ctx) {
    data class Props(val start: Int)

    private var count by value(props.start)

    override fun VDom.render() {
        div {
            h2 { +"Count: $count" }
            ui.blue.button {
                onClick { count++ }
                +"Increment"
            }
        }
    }
}
```

## Live examples

See Kraft in action at [peekandpoke.io/ultra/kraft/examples](https://peekandpoke.io/ultra/kraft/examples) — or run them
locally:

```bash
./gradlew -t :kraft:examples:hello-world:jsBrowserDevelopmentRun
./gradlew -t :kraft:examples:fomanticui:jsBrowserDevelopmentRun
./gradlew -t :kraft:examples:addons:jsBrowserDevelopmentRun
```

## Documentation

Full docs, guides, and API reference at **[peekandpoke.io/ultra/kraft](https://peekandpoke.io/ultra/kraft/)**.

## Installation

```kotlin
// build.gradle.kts (Kotlin Multiplatform, js target)
kotlin {
    sourceSets {
        jsMain {
            dependencies {
                implementation("io.peekandpoke.kraft:core:<version>")
                implementation("io.peekandpoke.kraft:semanticui:<version>")
            }
        }
    }
}
```

Check the [getting started guide](https://peekandpoke.io/ultra/kraft/getting-started) for the full setup.
