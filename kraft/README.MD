# Kraft — Build SPAs in Pure Kotlin

A component-based framework for building single-page applications.
Kotlin all the way down — no JavaScript, no template engines, no compromises.

Kraft compiles to JavaScript and uses [Preact](https://preactjs.com) (a 3KB React alternative) as its rendering engine —
but you never touch Preact directly.

## What you get

- **Components** — Class-based and functional, with props, state, and lifecycle hooks
- **Reactive State** — `var x by value(0)` — change the value, the UI updates
- **Routing** — Type-safe routes with params, middleware, and nested layouts
- **Forms & Validation** — Two-way binding, field validation, draft/commit pattern
- **SemanticUI DSL** — `ui.blue.button { +"Click" }` — a fluent API for FomanticUI
- **12 Addons** — ChartJS, PDF viewer, syntax highlighting, signature pad, and more

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
./gradlew -t --parallel :examples:hello-world:run
./gradlew -t --parallel :examples:fomanticui:run
./gradlew -t --parallel :examples:addons:run
```

## Documentation

Full docs, guides, and API reference at **[peekandpoke.io/ultra/kraft](https://peekandpoke.io/ultra/kraft/)**.

## Installation

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.peekandpoke.kraft:core:<version>")
    implementation("io.peekandpoke.kraft:semanticui:<version>")
}
```

Check the [getting started guide](https://peekandpoke.io/ultra/kraft/getting-started) for the full setup.
