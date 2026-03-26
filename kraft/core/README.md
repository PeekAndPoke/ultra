# Kraft Core

The core framework module — components, reactive state, routing, forms, data loading, and messaging.

## What it does

- **Components** — Class-based and functional components with props, state, and lifecycle hooks
- **Reactive State** — `var x by value(0)` — mutate the value, the UI re-renders
- **Routing** — Type-safe routes with parameters, middleware, and nested layouts
- **Forms & Validation** — Two-way binding, field validation, draft/commit pattern
- **Data Loading** — Async data streams that integrate with component lifecycle

## Quick example

```kotlin
class Counter(ctx: Ctx<Props>) : Component<Counter.Props>(ctx) {
    data class Props(val start: Int)

    private var count by value(props.start)

    override fun VDom.render() {
        div {
            h2 { +"Count: $count" }
            button {
                onClick { count++ }
                +"Increment"
            }
        }
    }
}
```

## Installation

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.peekandpoke.kraft:core:<version>")
}
```

## Documentation

Full docs at [peekandpoke.io/ultra/kraft](https://peekandpoke.io/ultra/kraft/).
