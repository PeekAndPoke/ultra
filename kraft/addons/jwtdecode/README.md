# Kraft Addon: JWT Decode

Kotlin/JS wrapper for [jwt-decode](https://github.com/auth0/jwt-decode) (npm: `jwt-decode`), providing client-side JWT
token decoding in Kraft applications.

## Features

- Decode JWT header and payload without signature verification
- Read token claims (subject, expiration, custom fields) as a map or a raw object
- Lightweight, no cryptographic dependencies

## Example

```kotlin
private val jwt: JwtDecodeAddon? by subscribingTo(addons.jwtDecode)

val token = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."

val claims = jwt?.decodeJwtAsMap(token)

println(claims?.get("sub"))
println(claims?.get("exp"))
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
- [Documentation](https://peekandpoke.io/ultra/kraft/addons)
