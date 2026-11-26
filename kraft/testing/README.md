# Kraft Testing

Testing utilities for Kraft components — render into a TestBed, query the DOM, simulate events.

## What it does

- **TestBed** — Renders a component tree into an isolated DOM for assertions
- **KQuery** — CSS-based selectors to find elements, read text, check attributes
- **Event simulation** — Click, input, change, and other DOM events
- **Async support** — Wait for re-renders and data loading before asserting

## Quick example

```kotlin
// In a kotest spec: TestBed.preact suspends
"counter increments" {
    TestBed.preact(
        view = { Counter(start = 0) },
    ) { root ->
        root.selectCss("h2").textContent() shouldBe "Count: 0"
        root.selectCss("button").click()
        delay(10)  // wait for re-render
        root.selectCss("h2").textContent() shouldBe "Count: 1"
    }
}
```

## Installation

```kotlin
// build.gradle.kts
jsTest {
    dependencies {
        implementation("io.peekandpoke.kraft:testing:<version>")
    }
}
```

## Documentation

Full docs at [peekandpoke.io/ultra/kraft/testing](https://peekandpoke.io/ultra/kraft/testing).
