# Kontainer: Dependency Injection

A lightweight DI container built around per-request scoping. Fresh containers per unit of work, stateful dynamic
services, and validated dependency graphs.

## Why Kontainer?

Most DI containers treat everything as a singleton or prototype. Kontainer adds a third concept: **Dynamic services**
that are scoped to a unit of work (e.g. a single HTTP request). They can hold state, carry context, and get cleaned up
automatically.

No annotations, no classpath scanning, no XML — just Kotlin code.

## Quick example

```kotlin
val blueprint = kontainer {
    singleton(Config::class)
    dynamic(RequestLogger::class)
    dynamic(UserSession::class)
    prototype(ReportGenerator::class)
}

// Per request: fresh container with fresh dynamic services
val kontainer = blueprint.create()
val logger = kontainer.get(RequestLogger::class)
```

## Key features

- **Two service types, four declarations**: Singleton and Prototype; declared as `singleton` (global), `dynamic`
  (per-container), `prototype` and `dynamicPrototype` (per-call)
- **Constructor injection**: services declare their dependencies in the primary constructor
- **Blueprint validation**: the dependency graph is validated on the first `create`
- **Per-request containers** — Fresh Dynamic services per unit of work, automatic cleanup
- **Semi-dynamic promotion** — Singletons that depend on Dynamic services are automatically promoted
- **Modules** — Group related services into reusable modules

## Documentation

Full docs at **[peekandpoke.io/ultra/kontainer](https://peekandpoke.io/ultra/kontainer/)**.

## Installation

```kotlin
// build.gradle.kts
dependencies {
    implementation("io.peekandpoke.ultra:kontainer:0.119.0")
}
```
