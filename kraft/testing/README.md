# Kraft Testing

Testing utilities for Kraft components — render into a TestBed, query the DOM, simulate events.

## What it does

- **TestBed** — Renders a component tree into an isolated DOM for assertions
- **KQuery** — CSS-based selectors to find elements, read text, check attributes
- **Event simulation** — Click, input, change, and other DOM events
- **Async support** — Wait for re-renders and data loading before asserting

## Quick example

```kotlin
@Test
fun counter_increments() = TestBed.preact {
    Counter(Counter.Props(start = 0))
} verify { root ->
    root.selectCss("h2").text shouldBe "Count: 0"
    root.selectCss("button").click()
    root.selectCss("h2").text shouldBe "Count: 1"
}
```

## Installation

```kotlin
// build.gradle.kts
dependencies {
    testImplementation("io.peekandpoke.kraft:testing:<version>")
}
```

## Documentation

Full docs at [peekandpoke.io/ultra/kraft/utilities](https://peekandpoke.io/ultra/kraft/utilities).
