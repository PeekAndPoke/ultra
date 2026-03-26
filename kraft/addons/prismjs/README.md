# Kraft Addon: PrismJS

Kotlin/JS wrapper for [PrismJS](https://prismjs.com/) (npm: `prismjs`), providing syntax highlighting in Kraft
applications.

## Features

- Syntax highlighting for 200+ languages
- Line numbers and line highlighting support
- Copy-to-clipboard integration

## Example

```kotlin
prismJs {
    language = "kotlin"
    code = """
        fun main() {
            println("Hello, world!")
        }
    """.trimIndent()
    showLineNumbers = true
}
```

## Installation

```xml
<dependency>
    <groupId>io.peekandpoke.kraft</groupId>
    <artifactId>addons-prismjs</artifactId>
    <version>${kraft.version}</version>
</dependency>
```

## Links

- [npm: prismjs](https://www.npmjs.com/package/prismjs)
- [Documentation](https://ultra.peekandpoke.io/kraft/addons/prismjs)
