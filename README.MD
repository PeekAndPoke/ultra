[![Kotlin](https://img.shields.io/badge/Kotlin-2.3.10-success.svg)](https://kotlinlang.org/docs/releases.html)
[![Maven Central](https://img.shields.io/maven-central/v/io.peekandpoke.ultra/common)](https://central.sonatype.com/namespace/io.peekandpoke.ultra)
[![License](https://img.shields.io/badge/License-Apache_2.0-blue.svg)](LICENSE)
[![CodeFactor](https://www.codefactor.io/repository/github/peekandpoke/ultra/badge)](https://www.codefactor.io/repository/github/peekandpoke/ultra)

# PeekAndPoke Ultra

A suite of Kotlin libraries that handle the hard parts for you.
Dependency injection, serialization, reactive state, SPAs, database access — all designed to work together, all
type-safe, all without the ceremony.

**Documentation: [peekandpoke.io](https://peekandpoke.io)**

## Libraries

### [Kontainer](https://peekandpoke.io/ultra/kontainer/) — Dependency Injection

Lightweight DI container built around per-request scoping. Fresh containers per unit of work, validated dependency
graphs. No annotations, no reflection, no runtime surprises.

```kotlin
val blueprint = kontainer {
    singleton(UserService::class)
    dynamic(RequestScope::class)
}

val kontainer = blueprint.create()
val service = kontainer.get(UserService::class)
```

### [Slumber](https://peekandpoke.io/ultra/slumber/) — Serialization

Kotlin-first serialization that understands nullable types, default values, and sealed classes. No annotations needed.
Two-pass error handling gives you speed when it works and precision when it doesn't.

```kotlin
val codec = Codec.default

val map = codec.slumber(User("Alice", 30))   // serialize
val user = codec.awake<User>(map)             // deserialize
```

### [Streams](https://peekandpoke.io/ultra/streams/) — Reactive Values

Lightweight reactive streams with always-has-a-value semantics. Composable operators, lazy subscriptions, automatic
cleanup. Powers Kraft's reactive state management.

```kotlin
val source = StreamSource(0)
val doubled = source.map { it * 2 }

doubled.subscribe { println(it) }
source(5)  // prints: 10
```

### [Karango](https://peekandpoke.io/ultra/karango/) — Type-Safe ArangoDB

Write AQL queries in Kotlin with full type safety. KSP generates compile-time checked property accessors. Repository
pattern with CRUD, hooks, and index management.

```kotlin
val adults = repo.findList {
    FOR(repo) { person ->
        FILTER(person.age GTE 18)
        SORT(person.name.ASC)
        RETURN(person)
    }
}
```

### [Kraft](https://peekandpoke.io/ultra/kraft/) — Build SPAs in Pure Kotlin

Component-based SPA framework using Preact under the hood. Type-safe HTML DSL, reactive state, routing, forms,
SemanticUI integration, and 12 addon wrappers.

```kotlin
class Counter(ctx: NoProps) : PureComponent(ctx) {
    private var count by value(0)

    override fun VDom.render() {
        ui.button {
            onClick { count++ }
            +"Clicked $count times"
        }
    }
}
```

**Example projects:**
[Hello World](https://github.com/PeekAndPoke/kraft-example-helloworld) |
[Router Demo](https://github.com/PeekAndPoke/kraft-example-router) |
[Remote API](https://github.com/PeekAndPoke/kraft-example-remote)

### [Mutator](https://peekandpoke.io/ultra/mutator/) — Mutate the Immutable

KSP-generated mutable wrappers for immutable data classes. Inspired by immer.js. Nested mutations, automatic change
propagation, dirty tracking — zero boilerplate.

```kotlin
// Without Mutator: nested copy() hell
val updated = company.copy(
    ceo = company.ceo.copy(
        address = company.ceo.address.copy(city = "Amsterdam")
    )
)

// With Mutator: just mutate it
val updated = company.mutate {
    ceo.address.city = "Amsterdam"
}
```

### Coming Soon

- **Funktor** — Full-stack Kotlin framework on Ktor (auth, REST, clustering, static web)

## Getting Started

All libraries are published on [Maven Central](https://central.sonatype.com/namespace/io.peekandpoke.ultra):

```kotlin
dependencies {
    implementation("io.peekandpoke.ultra:mutator-core:0.104.0")
    ksp("io.peekandpoke.ultra:mutator-ksp:0.104.0")
    implementation("io.peekandpoke.ultra:kontainer:0.104.0")
    implementation("io.peekandpoke.ultra:slumber:0.104.0")
    implementation("io.peekandpoke.ultra:streams:0.104.0")
    implementation("io.peekandpoke.ultra:karango-core:0.104.0")
    implementation("io.peekandpoke.kraft:core:0.104.0")
}
```

## For AI / LLMs

This project provides machine-readable documentation:

- [`llms.txt`](https://peekandpoke.io/llms.txt) — index with links to per-library docs
- [`llms-full.txt`](https://peekandpoke.io/llms-full.txt) — complete documentation in one file

## License

[Apache License 2.0](LICENSE)
