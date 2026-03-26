# Kraft Addon: JWT Decode

Kotlin/JS wrapper for [jwt-decode](https://github.com/auth0/jwt-decode) (npm: `jwt-decode`), providing client-side JWT
token decoding in Kraft applications.

## Features

- Decode JWT header and payload without signature verification
- Access token claims (subject, expiration, custom fields) in a type-safe way
- Lightweight, no cryptographic dependencies

## Example

```kotlin
val token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

val decoded = JwtDecode.decode(token)

println(decoded.subject)
println(decoded.expiration)
```

## Installation

```xml
<dependency>
    <groupId>io.peekandpoke.kraft</groupId>
    <artifactId>addons-jwtdecode</artifactId>
    <version>${kraft.version}</version>
</dependency>
```

## Links

- [npm: jwt-decode](https://www.npmjs.com/package/jwt-decode)
- [Documentation](https://ultra.peekandpoke.io/kraft/addons/jwtdecode)
