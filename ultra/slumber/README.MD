# Slumber — Serialization That Understands Kotlin

A serialization framework built for Kotlin's type system. Nullable types, default values, sealed classes — Slumber
handles them all without annotations.

## Why Slumber?

Jackson and Gson were built for Java. On their own they don't understand Kotlin's null safety, default parameter values,
or sealed class hierarchies. Slumber does — out of the box, with zero configuration for common cases.

## Quick example

```kotlin
import io.peekandpoke.ultra.slumber.Codec
import io.peekandpoke.ultra.slumber.awake
import io.peekandpoke.ultra.slumber.slumber

enum class Role { Viewer, Editor }

data class User(
    val name: String,
    val email: String,
    val role: Role = Role.Viewer,   // default values just work
    val tags: List<String> = listOf()
)

val codec = Codec.default

// Serialize
val map = codec.slumber(User("Alice", "alice@example.com"))

// Deserialize — missing fields use defaults, null safety is respected
val user = codec.awake<User>(mapOf("name" to "Bob", "email" to "bob@example.com"))
```

## Key features

- **Kotlin-first** — Nullable types, default values, and data classes just work
- **Sealed class polymorphism** — Automatic discriminators, no annotations needed
- **Codec system** — Pluggable codecs for custom type handling
- **Date/time support** — java.time, kotlinx.datetime, and MpDateTime out of the box
- **Error tracking** — Path-aware error reporting with graceful degradation
- **Extensible** — Custom modules for project-specific types

## Documentation

Full docs at **[peekandpoke.io/ultra/slumber](https://peekandpoke.io/ultra/slumber/)**.

## Installation

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.peekandpoke.ultra:slumber:<version>")
}
```
