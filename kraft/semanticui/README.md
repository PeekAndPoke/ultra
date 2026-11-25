# Kraft SemanticUI

A fluent Kotlin DSL for building UIs with [FomanticUI](https://fomantic-ui.com) (the maintained fork of SemanticUI).

## What it does

- **Fluent element DSL**: `ui.blue.button { +"Click" }`, `ui.three.column.grid { ... }`
- **Modal dialogs**: `OkCancelModal`, and `FadingModal` for your own modal components
- **Popups and context menus**: Semantic UI styling for Kraft's popups, hover popup helpers (`topLeft`, `topCenter`,
  `topRight`)
- **Drag & drop**: `DropZoneState`, Fomantic classes for Kraft's drop zones
- **Form fields and more**: input, textarea and checkbox fields, pagination, a dropdown menu, a collapsable component

## Quick example

```kotlin
override fun VDom.render() {
    ui.container {
        ui.header H1 { +"Dashboard" }
        ui.three.column.grid {
            noui.column {
                ui.blue.card {
                    noui.content { ui.header { +"Users" } }
                }
            }
        }
        ui.green.button {
            onClick { save() }
            +"Save"
        }
    }
}
```

## Installation

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.peekandpoke.kraft:semanticui:<version>")
}
```

## Documentation

Full docs at [peekandpoke.io/ultra/kraft/semantic-ui](https://peekandpoke.io/ultra/kraft/semantic-ui).
