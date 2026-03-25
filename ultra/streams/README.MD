# Streams — Reactive Streams That Always Have a Value

A lightweight reactive streams library for Kotlin Multiplatform. Always-has-a-value semantics, lazy subscriptions, and
composable operators — no marble diagrams required.

## Why Streams?

Most reactive libraries are built around event streams that may or may not have a current value. Streams takes a
different approach: every stream always has a value. This eliminates an entire class of bugs around initialization and
null states.

## Quick example

```kotlin
val count = streamOf(0)
val doubled = count.map { it * 2 }
val label = doubled.map { "Value: $it" }

label.onChange { println(it) }

count.next(5)  // prints "Value: 10"
```

## Key features

- **Always has a value** — No empty states, no initialization race conditions
- **Kotlin Multiplatform** — Works on JVM and JS
- **Composable operators** — map, filter, combine, debounce, distinctUntilChanged, and more
- **Lazy subscriptions** — Operators only compute when someone is listening
- **Kraft integration** — First-class support as component state in Kraft SPAs
- **Lightweight** — No heavy framework dependencies

## Documentation

Full docs at **[peekandpoke.io/ultra/streams](https://peekandpoke.io/ultra/streams/)**.

## Installation

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.peekandpoke.ultra:streams:<version>")
}
```
