# Kraft Addon: Avatars

Kotlin/JS wrapper for [minidenticons](https://github.com/laurentpayot/minidenticons) (npm: `minidenticons`), providing
deterministic SVG avatar generation in Kraft applications.

## Features

- Generate unique SVG avatars from any input string (e.g., username, email)
- Consistent output: the same input always produces the same avatar
- Lightweight, no external image assets required

## Example

```kotlin
minidenticon {
    identity = "user@example.com"
    saturation = 50
    lightness = 50
}
```

## Installation

```xml
<dependency>
    <groupId>io.peekandpoke.kraft</groupId>
    <artifactId>addons-avatars</artifactId>
    <version>${kraft.version}</version>
</dependency>
```

## Links

- [npm: minidenticons](https://www.npmjs.com/package/minidenticons)
- [Documentation](https://ultra.peekandpoke.io/kraft/addons/avatars)
