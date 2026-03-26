# Kraft SemanticUI

A fluent Kotlin DSL for building UIs with [FomanticUI](https://fomantic-ui.com) (the maintained fork of SemanticUI).

## What it does

- **Fluent element DSL** — `ui.blue.button { +"Click" }`, `ui.three.column.grid { ... }`
- **Modal dialogs** — `OkCancelModal`, `ConfirmModal`, and custom modal components
- **Context menus** — Right-click menus with nested items
- **Drag & drop** — Built-in support for sortable and droppable elements
- **Full coverage** — Buttons, forms, tables, menus, accordions, dropdowns, and more

## Quick example

```kotlin
override fun VDom.render() {
    ui.container {
        ui.header H1 { +"Dashboard" }
        ui.three.column.grid {
            column {
                ui.blue.card {
                    content { header { +"Users" } }
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
