# Kraft Addon: NX Compile

Kotlin/JS wrapper for [@nx-js/compiler-util](https://github.com/nx-js/compiler-util), providing sandboxed JavaScript
code execution in Kraft applications.

## Features

- Compile and execute JavaScript code strings in a sandboxed context
- Control which variables and functions are exposed to the sandbox
- Isolate untrusted code from the main application scope

## Example

```kotlin
private val nx: NxCompileAddon? by subscribingTo(addons.nxCompile)

val compiled = nx?.compileCode("x + y")
val result = compiled?.invoke(js("({ x: 10, y: 20 })"))
// result: 30
```

## Installation

```xml
<dependency>
    <groupId>io.peekandpoke.kraft</groupId>
    <artifactId>addons-nxcompile</artifactId>
    <version>${kraft.version}</version>
</dependency>
```

## Links

- [npm: @nx-js/compiler-util](https://www.npmjs.com/package/@nx-js/compiler-util)
- [Documentation](https://peekandpoke.io/ultra/kraft/addons)
