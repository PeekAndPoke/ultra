# Kraft Addon: NX Compile

Kotlin/JS wrapper for [@nx-js/compiler-util](https://github.com/nx-js/compiler-util), running JavaScript code strings
against a context object in Kraft applications.

## Features

- `compileCode(code)` turns a JavaScript code string into a function that runs it against a context object
- Names in the code resolve on the context object; globals are hidden behind a proxy
- Not a security sandbox: never run untrusted code with it

## Example

```kotlin
private val nx: NxCompileAddon? by subscribingTo(addons.nxCompile)

val code: dynamic = nx?.compileCode("return x + y")
val result = if (code != null) code(js("({ x: 10, y: 20 })")) else null
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
