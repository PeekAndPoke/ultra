# Kraft Addon: Sourcemapped Stacktrace

Kotlin/JS wrapper for [sourcemapped-stacktrace](https://github.com/nicksrandall/sourcemapped-stacktrace) (npm:
`sourcemapped-stacktrace`), providing source-mapped stack trace resolution in Kraft applications.

## Features

- Map minified/compiled JavaScript stack traces back to original source locations
- Resolve Kotlin/JS stack traces to readable file names and line numbers
- Useful for client-side error reporting and debugging

## Example

```kotlin
SourcemappedStacktrace.mapStackTrace(error.stackTraceToString()) { mapped ->
    mapped.forEach { frame ->
        console.log(frame)
    }
}
```

## Installation

```xml
<dependency>
    <groupId>io.peekandpoke.kraft</groupId>
    <artifactId>addons-sourcemappedstacktrace</artifactId>
    <version>${kraft.version}</version>
</dependency>
```

## Links

- [npm: sourcemapped-stacktrace](https://www.npmjs.com/package/sourcemapped-stacktrace)
- [Documentation](https://ultra.peekandpoke.io/kraft/addons/sourcemappedstacktrace)
