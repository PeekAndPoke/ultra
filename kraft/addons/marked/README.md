# Kraft Addon: Marked

Kotlin/JS wrapper for [marked](https://marked.js.org/) (npm: `marked`)
with [DOMPurify](https://github.com/cure53/DOMPurify) sanitization, providing Markdown rendering in Kraft applications.

## Features

- Convert Markdown to HTML using the marked parser
- Sanitize output with DOMPurify to prevent XSS
- Render Markdown content directly as Kraft components

## Example

```kotlin
markdown {
    content = """
        ## Hello

        This is **bold** and this is a [link](https://example.com).
    """.trimIndent()
}
```

## Installation

```xml
<dependency>
    <groupId>io.peekandpoke.kraft</groupId>
    <artifactId>addons-marked</artifactId>
    <version>${kraft.version}</version>
</dependency>
```

## Links

- [npm: marked](https://www.npmjs.com/package/marked)
- [Documentation](https://ultra.peekandpoke.io/kraft/addons/marked)
