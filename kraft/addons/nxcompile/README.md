# Kraft Addon: NX Compile

Kotlin/JS wrapper for [@nx-js/compiler-util](https://github.com/nicksrandall/nx-compile), providing sandboxed JavaScript
code execution in Kraft applications.

## Features

- Compile and execute JavaScript code strings in a sandboxed context
- Control which variables and functions are exposed to the sandbox
- Isolate untrusted code from the main application scope

## Example

```kotlin
val code = "return x + y"
val compiled = NxCompile.compile(code)

val result = compiled.exec(
    mapOf("x" to 10, "y" to 20)
)
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

- [npm: @aspect-build/rules_js](https://www.npmjs.com/package/@aspect-build/rules_js)
- [Documentation](https://ultra.peekandpoke.io/kraft/addons/nxcompile)
