## Streams — Reactive Values

### Overview {#streams-overview}

# Streams — Reactive Values, Simplified

A lightweight reactive streams library for Kotlin Multiplatform. Every stream always has a value. No marble diagrams
required.

## The core idea: streams always have a value

Most reactive libraries (RxJava, RxJS, Kotlin Flow) model streams as sequences that *might eventually emit* something.
You subscribe, wait, and hope. This creates a maze of operators for handling the "not yet" state — `startWith`,
`defaultIfEmpty`, `firstOrDefault`, `onErrorResumeNext` — each with its own marble diagram.

Streams takes a radically simpler approach: **every stream always has a current value**. You can read it at any time
with `stream()`. When you subscribe, you immediately get the current value — no waiting, no special handling.

```kotlin
val source = StreamSource(42)

// Always has a value — read it anytime
println(source())  // 42

// Subscribe — immediately called with current value
source.subscribeToStream { value ->
    println("Got: $value")
}
// prints "Got: 42" immediately

// Update — all subscribers notified
source(99)
// prints "Got: 99"
```

This is what makes Streams perfect for UI programming. In Kraft, components subscribe to streams and re-render when
values change. Because streams always have a value, the first render always has data — no loading spinners, no null
checks, no "initial state" boilerplate.

## Two types

The entire library is built on two types:

- **`Stream<T>`** — read-only. Has `invoke(): T` (get value) and `subscribeToStream()` (observe changes).
- **`StreamSource<T>`** — read-write. Adds `invoke(next: T)` (set value), `modify()`, and `reset()`.

Operators transform a `Stream` into another `Stream`. The chain is lazy — operators only subscribe upstream when they
have downstream subscribers.

## What you get

- **Always Has a Value** — `stream()` never blocks, never returns null (unless T is nullable). No marble diagrams
  needed.
- **Composable Operators** — `map`, `filter`, `combine`, `debounce`, `fold`, `distinct`, `history`, and more.
- **Lazy Subscriptions** — Operators only subscribe upstream when they have downstream subscribers. No wasted work.
- **Kotlin Multiplatform** — Core + all operators work on JVM and JS. Plus JS-specific extras like `animTicker()` and
  localStorage persistence.
- **Resilient** — A failing subscriber never prevents other subscribers from being notified. Streams keep flowing.
- **Flow Interop** — `stream.asFlow()` bridges into Kotlin's Flow world when you need it.

## Quick taste

```kotlin
val name = StreamSource("World")
val greeting = name.map { "Hello, $it!" }

greeting.subscribeToStream { println(it) }
// prints "Hello, World!"

name("Kotlin")
// prints "Hello, Kotlin!"

// Combine two streams
val firstName = StreamSource("Jane")
val lastName = StreamSource("Doe")

val fullName = firstName.pairedWith(lastName).map { (f, l) -> "$f $l" }
println(fullName())  // "Jane Doe"

firstName("John")
println(fullName())  // "John Doe"
```

---

### Getting Started {#streams-getting-started}

# Getting Started

Add Streams to your project and create your first reactive values.

## 1. Add the dependency

In your `build.gradle.kts`:

```kotlin
dependencies {
    implementation("io.peekandpoke.ultra:streams:0.102.0")
}
```

Find the latest version on [Maven Central](https://central.sonatype.com/namespace/io.peekandpoke.ultra).

Streams is a Kotlin Multiplatform library — it works in `commonMain`, `jvmMain`, and `jsMain`.

## 2. Create a StreamSource

A `StreamSource` is a mutable reactive value. It always has a current value.

```kotlin
import io.peekandpoke.ultra.streams.StreamSource

val counter = StreamSource(0)

// Read the current value
println(counter())  // 0

// Set a new value
counter(1)
println(counter())  // 1

// Modify based on current value
counter.modify { this + 1 }
println(counter())  // 2

// Reset to initial
counter.reset()
println(counter())  // 0
```

For a stream that never changes — a constant value wrapped in a `Stream<T>` — use `steady()`:

```kotlin
import io.peekandpoke.ultra.streams.steady

val pi = steady(3.14)

println(pi())  // 3.14

// Subscribers are called once with the value and never again
pi.subscribeToStream { println(it) }  // prints "3.14"
```

Handy when an API wants a `Stream<T>` but you only have a plain value — no need to create a `StreamSource` just to never
update it.

## 3. Subscribe to changes

Subscribing immediately delivers the current value, then every subsequent change:

```kotlin
val name = StreamSource("Alice")

val unsubscribe = name.subscribeToStream { value ->
    println("Name is: $value")
}
// prints "Name is: Alice" immediately

name("Bob")
// prints "Name is: Bob"

name("Charlie")
// prints "Name is: Charlie"

// Stop listening
unsubscribe()
```

## 4. Transform with operators

Operators create new read-only streams derived from a source:

```kotlin
val source = StreamSource(5)

// map — transform each value
val doubled = source.map { it * 2 }
println(doubled())  // 10

// filter — only pass values matching a predicate
val big = source.filter(initial = 0) { it > 10 }
println(big())  // 0 (initial, since 5 doesn't match)

source(20)
println(big())     // 20
println(doubled()) // 40
```

## 5. Combine streams

Merge two streams into one:

```kotlin
val a = StreamSource(1)
val b = StreamSource(10)

val sum = a.combinedWith(b) { x, y -> x + y }
println(sum())  // 11

a(5)
println(sum())  // 15

b(100)
println(sum())  // 105
```

## How subscriptions work

Streams use **lazy subscriptions**. When you chain operators like `source.map { }.filter { }`, no work happens until
someone subscribes to the end of the chain. The subscription propagates upstream — and when the last subscriber
unsubscribes, the entire chain tears down.

This means unused streams cost nothing.

---

### Operators {#streams-operators}

# Operators

Every operator takes a stream and returns a new stream. Chain them freely.

## steady

Not an operator on an existing stream — a factory for a `Stream<T>` that always holds the same value. It never changes
and never notifies beyond the initial delivery.

```kotlin
import io.peekandpoke.ultra.streams.steady

val pi = steady(3.14)

println(pi())  // 3.14

// Subscribers are called once with the value and never again
pi.subscribeToStream { println(it) }  // prints "3.14"
```

Handy when an API wants a `Stream<T>` but you only have a plain value — no need to spin up a `StreamSource` just to
never update it.

## map

Transform each value:

```kotlin
val source = StreamSource(5)
val doubled = source.map { it * 2 }

println(doubled())  // 10

source(10)
println(doubled())  // 20
```

## onEach

Execute a side effect for each value without changing it. Useful for logging:

```kotlin
val source = StreamSource(1)

val stream = source
    .onEach { println("Saw: $it") }
    .map { it * 10 }
```

## indexed

Pairs each value with its index (starting at 0):

```kotlin
val source = StreamSource("a")
val indexed = source.indexed()

// Pair(0, "a"), then Pair(1, "b"), Pair(2, "c")...
```

## filter

Only pass values matching a predicate. Since streams always have a value, you provide an initial value for when nothing
matches yet:

```kotlin
val source = StreamSource(3)

val big = source.filter(initial = 0) { it > 10 }
println(big())  // 0

source(20)
println(big())  // 20

source(5)
println(big())  // 20 (keeps last matching value)
```

Variants:

```kotlin
// Nullable — returns null when no value matches yet
val nullable = source.filter { it > 10 }  // Stream<Int?>

// Filter nulls out of a nullable stream
val nonNull = nullableStream.filterNotNull()

// Filter by type
val strings = mixedStream.filterIsInstance<String>()
```

## distinct

Only publish when the value actually changes:

```kotlin
val source = StreamSource(1)
val distinct = source.distinct()

val received = mutableListOf<Int>()
distinct.subscribeToStream { received.add(it) }

source(1)  // same — skipped
source(2)  // different — published
source(2)  // same — skipped
source(3)  // different — published

// received = [1, 2, 3]
```

Use `distinctStrict()` for reference equality (`!==`) instead of structural equality (`!=`).

## fallback

Replace null values with a default:

```kotlin
val source = StreamSource<String?>(null)

val safe = source.fallbackTo("default")
println(safe())  // "default"

source("hello")
println(safe())  // "hello"

// Or compute the fallback dynamically
val computed = source.fallbackBy { "fallback-value" }
```

---

### Combining & Accumulating {#streams-combining}

# Combining & Accumulating

Merge streams together and build up state over time.

## combinedWith

Merge two streams with a combinator function. Publishes whenever either source changes:

```kotlin
val a = StreamSource(1)
val b = StreamSource(10)

val sum = a.combinedWith(b) { x, y -> x + y }
println(sum())  // 11

a(5)
println(sum())  // 15

b(100)
println(sum())  // 105
```

## pairedWith

Shorthand for combining into a `Pair`:

```kotlin
val pair = a.pairedWith(b)
println(pair())  // Pair(1, 10)
```

## fold

Accumulate values over time, like `List.fold()` but reactive:

```kotlin
val clicks = StreamSource("click")
val count = clicks.fold(0) { acc, _ -> acc + 1 }

// count is 1 after initial subscribe, increments on each "click"
```

`foldNotNull()` skips null values in the accumulation.

## history

Keep a sliding window of recent values:

```kotlin
val source = StreamSource(1)
val recent = source.history(3)

source(2)
source(3)
source(4)

println(recent())  // [2, 3, 4] — oldest values dropped
```

`historyOfNonNull(capacity)` only keeps non-null values.

---

### Control & Timing {#streams-control}

# Control & Timing

Rate-limit, pause, and schedule stream emissions.

## debounce

Only publish after a quiet period. When values arrive rapidly, only the last one is emitted after the delay:

```kotlin
val search = StreamSource("")
val debounced = search.debounce(300)  // 300ms delay

// Typing rapidly: "h", "he", "hel", "hell", "hello"
// Only "hello" is published — 300ms after the last keystroke
```

## cutoffWhen / cutoffWhenNot

Pause a stream based on a boolean predicate stream. While paused, the source is fully unsubscribed. When resumed, the
current value is published immediately:

```kotlin
val source = StreamSource(1)
val paused = StreamSource(false)

val stream = source.cutoffWhen(paused)

// stream publishes values while paused() is false
source(2)  // published

paused(true)
source(3)  // NOT published — stream is cut off

paused(false)
// immediately publishes source's current value (3)
```

## ticker

Emits a `TickerFrame` at regular intervals. Only runs while subscribed:

```kotlin
import io.peekandpoke.ultra.streams.ops.ticker

val tick = ticker(1000)  // every 1000ms

val unsub = tick.subscribeToStream { frame ->
    println("Tick #${frame.count}, delta: ${frame.deltaTime}ms")
}

// ... later
unsub()  // stops the ticker
```

Also accepts a `Duration`: `ticker(1.seconds)`.

## mapAsync

Maps values asynchronously. The result stream is nullable — `null` until the first async operation completes:

```kotlin
val userId = StreamSource("user-1")

val user = userId.mapAsync { id ->
    api.fetchUser(id)  // suspend function
}

// user() is null initially, then the fetched User object
```

---

### Subscriptions & Resilience {#streams-subscriptions}

# Subscriptions & Resilience

Batch cleanup of subscriptions and protection against failing handlers.

## The Subscriptions collector

Forgetting to unsubscribe is the most common source of leaks in reactive code. The `Subscriptions` class collects all
your subscriptions and cleans them up in a single call:

```kotlin
import io.peekandpoke.ultra.streams.Subscriptions

val subs = Subscriptions()

// Subscribe via the collector
subs.subscribe(nameStream) { println("Name: $it") }
subs.subscribe(ageStream) { println("Age: $it") }

// Or use the += operator
subs += counterStream.subscribeToStream { println("Count: $it") }

println(subs.size)  // 3

// Clean up everything at once
subs.unsubscribeAll()
```

This is especially useful in UI components:

```kotlin
class UserDashboard {
    private val subs = Subscriptions()

    fun mount(userStream: Stream<User>, notificationsStream: Stream<List<Notification>>) {
        subs.subscribe(userStream) { renderHeader(it) }
        subs.subscribe(notificationsStream) { renderNotifications(it) }
    }

    fun unmount() {
        subs.unsubscribeAll()
    }
}
```

`unsubscribeAll()` can be called multiple times safely.

## Error resilience

A failing subscriber never breaks other subscribers. If one handler throws during a publish cycle, the remaining
handlers still receive the value:

```kotlin
val source = StreamSource(1)

source.subscribeToStream { throw RuntimeException("oops") }
source.subscribeToStream { println("Got: $it") }

source(2)  // prints "Got: 2"
```

The exception is logged as `[Streams] ERROR: subscriber threw ...` so it shows up in your console during development,
but it never takes down the stream.

## Lazy subscription chains

Operator chains are fully lazy. No work happens until someone subscribes to the end of the chain:

```kotlin
val source = StreamSource(1)

// No subscriptions created yet — this chain is inert
val result = source
    .map { it * 2 }
    .filter(0) { it > 5 }
    .distinct()

// NOW the chain subscribes upstream
val unsub = result.subscribeToStream { println(it) }

// Unsubscribing tears down the entire chain
unsub()
// source has zero subscribers again
```

Each operator only subscribes to its upstream when it gains its first subscriber, and unsubscribes when its last
subscriber leaves. Unused branches cost nothing.

---

### JS-Specific Operators {#streams-js-specific}

# JS-Specific Operators

These operators are only available in `jsMain` because they use browser APIs.

## animTicker()

Emits a `TickerFrame` on every `requestAnimationFrame` callback (~60fps). Perfect for animations and frame-based game
loops:

```kotlin
import io.peekandpoke.ultra.streams.ops.animTicker

val tick = animTicker()

val unsub = tick.subscribeToStream { frame ->
    // frame.count     — frames since start
    // frame.deltaTime — ms since last frame
    updateAnimation(frame.deltaTime)
}

unsub()  // stops requesting animation frames
```

Unlike `ticker(intervalMs)` (available on all platforms), `animTicker()` synchronizes with the browser's display refresh
rate.

## persistInLocalStorage()

Persists a stream's value in localStorage. On page reload, the stream initializes from storage:

```kotlin
import io.peekandpoke.ultra.streams.ops.persistInLocalStorage
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer

val theme = StreamSource("light")
    .persistInLocalStorage("app.theme", String.serializer())

@Serializable
data class Preferences(val fontSize: Int = 14, val darkMode: Boolean = false)

val prefs = StreamSource(Preferences())
    .persistInLocalStorage("app.prefs", Preferences.serializer())
```

Falls back to in-memory storage silently if localStorage is unavailable or the quota is exceeded. Accepts a custom
`StringFormat` codec (defaults to JSON with `ignoreUnknownKeys = true`).

## debouncedFunc() / debouncedFuncExceptFirst()

Standalone debounced function wrappers using browser `setTimeout`. Useful outside of streams:

```kotlin
import io.peekandpoke.ultra.streams.ops.debouncedFunc
import io.peekandpoke.ultra.streams.ops.debouncedFuncExceptFirst

val saveSearch = debouncedFunc(delayMs = 200) {
    performSearch(inputField.value)
}

inputField.onInput { saveSearch() }

// Execute first call immediately, debounce the rest
val autoSave = debouncedFuncExceptFirst(delayMs = 1000) {
    saveDraft()
}
```

---

### Extending Streams {#streams-extending}

# Extending Streams

Build your own operators, bridge to Kotlin Flow, and understand the platform matrix.

## Custom operators

Build operators by extending `StreamMapper` (transform) or `StreamWrapper` (pass-through):

```kotlin
import io.peekandpoke.ultra.streams.Stream
import io.peekandpoke.ultra.streams.StreamMapper

fun Stream<Int>.clamp(min: Int, max: Int): Stream<Int> =
    StreamMapper(
        wrapped = this,
        mapper = { it.coerceIn(min, max) }
    )

val source = StreamSource(50)
val clamped = source.clamp(0, 100)

source(150)
println(clamped())  // 100
```

For operators with internal state, extend `StreamWrapperBase` directly and override `handleIncoming()` and `invoke()`.

## Flow interop

Convert any stream to a Kotlin `Flow`:

```kotlin
import io.peekandpoke.ultra.streams.ops.asFlow

val source = StreamSource(1)

source.asFlow()
    .filter { it > 5 }
    .take(3)
    .collect { println(it) }
```

The Flow emits the current value immediately on collection, then subsequent values. Cancelling the collector
unsubscribes automatically.

## Platform summary

| Operator                                                                         | Platform                          |
|----------------------------------------------------------------------------------|-----------------------------------|
| map, filter, combine, fold, distinct, fallback, history, indexed, onEach, cutoff | All platforms                     |
| ticker, debounce, mapAsync, asFlow                                               | All platforms (coroutine-based)   |
| `animTicker()`                                                                   | JS only (`requestAnimationFrame`) |
| `persistInLocalStorage()`                                                        | JS only (`localStorage`)          |
| `debouncedFunc()`, `debouncedFuncExceptFirst()`                                  | JS only (`setTimeout`)            |

---
