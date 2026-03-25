## Slumber — Serialization

### Overview {#slumber-overview}

# Slumber — Serialization That Understands Kotlin

A serialization framework built for Kotlin's type system. Nullable types, default values, sealed classes — Slumber
handles them all without annotations or configuration.

## The Problem

You have a Kotlin data class. You want to serialize it to a Map (for a database, an API, a cache). Jackson needs
annotations. Gson ignores nullability. kotlinx.serialization needs a compiler plugin.

And when deserialization fails, you get a cryptic exception with no idea which nested field caused it.

## The Solution

```kotlin
data class User(val name: String, val age: Int, val email: String? = null)

val codec = Codec.default

// Serialize ("slumber")
val map = codec.slumber(User("Alice", 30))
// -> { "name": "Alice", "age": 30, "email": null }

// Deserialize ("awake")
val user = codec.awake<User>(mapOf("name" to "Bob", "age" to 25))
// -> User(name="Bob", age=25, email=null)
```

That's it. No annotations needed. No compiler plugin. Slumber uses Kotlin reflection to understand your data classes —
nullable fields, default values, generics, and all.

## What Makes It Different

- **Kotlin-First** — Understands `String` vs `String?`, default parameters, data classes, sealed classes — at the type
  level.
- **Graceful Degradation** — Nullable field receives bad data? Returns `null` instead of crashing. Required field fails?
  Detailed path in the error.
- **Two-Pass Error Handling** — Fast pass first, no overhead. Only builds full diagnostic path on failure — performance
  when it works, clarity when it doesn't.
- **Format-Agnostic** — Works with Maps and Lists, not JSON strings. Use it for databases, REST APIs, caches, or any
  storage backend.
- **Pluggable Modules** — Custom type handling via `SlumberModule`. No annotation processors — just implement an
  interface.
- **Polymorphism Built-In** — Sealed classes auto-detected. Custom discriminators, migration aliases, default fallback
  types — all supported.

## The Naming

Slumber uses a playful metaphor:

- **Slumber** = serialize. Put your objects to sleep as raw data.
- **Awake** = deserialize. Wake your data back up as typed objects.

## Quick Taste

Polymorphic sealed classes — zero configuration:

```kotlin
sealed class Event {
    data class UserCreated(val userId: String) : Event()
    data class UserDeleted(val userId: String, val reason: String?) : Event()
}

val codec = Codec.default

// Slumber a list of mixed types
val data = codec.slumber(listOf(
    Event.UserCreated("alice"),
    Event.UserDeleted("bob", reason = "inactive"),
))
// -> [
//   { "_type": "...UserCreated", "userId": "alice" },
//   { "_type": "...UserDeleted", "userId": "bob", "reason": "inactive" }
// ]

// Awake it back — types are restored automatically
val events = codec.awake<List<Event>>(data)
```

---

### Getting Started {#slumber-getting-started}

# Getting Started

Add the dependency, create a codec, serialize your first object.

## 1. Add the dependency

In your `build.gradle.kts`:

```kotlin
dependencies {
    implementation("io.peekandpoke.ultra:slumber:0.102.0")
}
```

Slumber requires **Kotlin reflection** (included transitively) and runs on the **JVM**.

## 2. Create a codec

```kotlin
import de.peekandpoke.ultra.slumber.Codec

// The default codec handles: primitives, data classes, enums, collections,
// sealed classes, java.time.*, kotlinx.datetime.*, and MpDateTime types.
val codec = Codec.default
```

## 3. Serialize (slumber)

```kotlin
data class Address(val city: String, val zip: String)
data class Person(val name: String, val age: Int, val address: Address)

val alice = Person("Alice", 30, Address("Berlin", "10115"))

val data = codec.slumber(alice)
// -> {
//   "name": "Alice",
//   "age": 30,
//   "address": { "city": "Berlin", "zip": "10115" }
// }
```

The result is a `Map<String, Any?>` — not a JSON string. You can pass it to any storage backend, JSON encoder, or
database driver.

## 4. Deserialize (awake)

```kotlin
import de.peekandpoke.ultra.slumber.awake

val input = mapOf(
    "name" to "Bob",
    "age" to 25,
    "address" to mapOf("city" to "Munich", "zip" to "80331"),
)

val person = codec.awake<Person>(input)
// -> Person(name="Bob", age=25, address=Address(city="Munich", zip="80331"))
```

## 5. Round-trip

```kotlin
val original = Person("Alice", 30, Address("Berlin", "10115"))
val restored = codec.awake<Person>(codec.slumber(original))

assert(restored == original) // true
```

## How it handles Kotlin features

### Nullable fields

```kotlin
data class Profile(val name: String, val bio: String?)

// Missing nullable field -> null
codec.awake<Profile>(mapOf("name" to "Alice"))
// -> Profile(name="Alice", bio=null)

// Explicit null -> null
codec.awake<Profile>(mapOf("name" to "Alice", "bio" to null))
// -> Profile(name="Alice", bio=null)

// Invalid data on nullable field -> null (graceful degradation)
codec.awake<Profile>(mapOf("name" to "Alice", "bio" to listOf(1, 2, 3)))
// -> Profile(name="Alice", bio=null)
```

### Default parameters

```kotlin
data class Config(
    val host: String = "localhost",
    val port: Int = 8080,
    val debug: Boolean = false,
)

// Missing fields use defaults
codec.awake<Config>(mapOf("debug" to true))
// -> Config(host="localhost", port=8080, debug=true)

// Empty map -> all defaults
codec.awake<Config>(emptyMap<String, Any>())
// -> Config(host="localhost", port=8080, debug=false)
```

### Type coercion

```kotlin
data class Stats(val count: Int, val label: String)

// String "42" -> Int 42, Number 100L -> Int 100
codec.awake<Stats>(mapOf("count" to "42", "label" to 100))
// -> Stats(count=42, label="100")

// Works for all numeric types: Int, Long, Double, Float, Short, Byte
```

---

### Data Classes {#slumber-data-classes}

# Data Classes

Slumber serializes data classes by reflecting on the primary constructor. No annotations needed for standard cases.

## Basic data classes

Every constructor parameter becomes a field in the serialized output:

```kotlin
data class User(val name: String, val age: Int)

val codec = Codec.default

codec.slumber(User("Alice", 30))
// -> { "name": "Alice", "age": 30 }

codec.awake<User>(mapOf("name" to "Bob", "age" to 25))
// -> User(name="Bob", age=25)
```

## Nested data classes

Nesting works recursively — no depth limit:

```kotlin
data class Address(val city: String, val zip: String)
data class Company(val name: String, val hq: Address)
data class Employee(val name: String, val company: Company)

val emp = Employee("Alice", Company("ACME", Address("Berlin", "10115")))

codec.slumber(emp)
// -> {
//   "name": "Alice",
//   "company": {
//     "name": "ACME",
//     "hq": { "city": "Berlin", "zip": "10115" }
//   }
// }
```

## Enums

Enums serialize to their name as a string:

```kotlin
enum class Role { ADMIN, USER, GUEST }
data class Account(val name: String, val role: Role)

codec.slumber(Account("Alice", Role.ADMIN))
// -> { "name": "Alice", "role": "ADMIN" }

codec.awake<Account>(mapOf("name" to "Bob", "role" to "USER"))
// -> Account(name="Bob", role=Role.USER)
```

## Kotlin object singletons

Kotlin `object` singletons are preserved — the same instance is returned on deserialization:

```kotlin
object AppConfig {
    val version = "1.0"
}

val data = codec.slumber(AppConfig)
// -> {} (empty map)

val restored = codec.awake<AppConfig>(data)
// restored === AppConfig (same instance)
```

## Computed fields with @Slumber.Field

By default, only constructor parameters are serialized. To include computed properties, use `@Slumber.Field`:

```kotlin
data class Invoice(val items: List<Double>, val taxRate: Double) {
    @Slumber.Field
    val subtotal get() = items.sum()

    @Slumber.Field
    val tax get() = subtotal * taxRate

    @Slumber.Field
    val total get() = subtotal + tax
}

codec.slumber(Invoice(listOf(10.0, 20.0, 30.0), 0.19))
// -> {
//   "items": [10.0, 20.0, 30.0],
//   "taxRate": 0.19,
//   "subtotal": 60.0,
//   "tax": 11.4,
//   "total": 71.4
// }
```

`@Slumber.Field` works on properties, getters, and `lazy` fields. Only serialization is affected — deserialization still
uses the constructor.

## Private constructors

Data classes with private constructors still work — Slumber uses Kotlin reflection to access them:

```kotlin
data class UserId private constructor(val value: String) {
    companion object {
        fun of(raw: String) = UserId(raw.trim().lowercase())
    }
}

// Deserialization bypasses the companion factory
codec.awake<UserId>(mapOf("value" to "ABC-123"))
// -> UserId(value="ABC-123")
```

## Generic data classes

Full generic support — type parameters are preserved through serialization:

```kotlin
data class Wrapper<T>(val value: T)
data class Pair<A, B>(val first: A, val second: B)

codec.awake<Wrapper<Int>>(mapOf("value" to 42))
// -> Wrapper(value=42)

codec.awake<Pair<String, Int>>(mapOf("first" to "hello", "second" to 99))
// -> Pair(first="hello", second=99)
```

---

### Polymorphism {#slumber-polymorphism}

# Polymorphism

Slumber auto-detects sealed classes, adds type discriminators, and supports custom identifiers, default fallback types,
and migration aliases.

## Sealed classes — zero config

Sealed classes just work. Slumber adds a `_type` discriminator automatically:

```kotlin
sealed class Shape {
    data class Circle(val radius: Double) : Shape()
    data class Rectangle(val width: Double, val height: Double) : Shape()
}

val codec = Codec.default

// Slumber adds "_type" with the fully qualified class name
codec.slumber(Shape.Circle(5.0))
// -> { "_type": "com.example.Shape.Circle", "radius": 5.0 }

// Awake reads "_type" and instantiates the correct subclass
codec.awake<Shape>(mapOf(
    "_type" to "com.example.Shape.Circle",
    "radius" to 5.0,
))
// -> Shape.Circle(radius=5.0)
```

## Custom identifiers with @SerialName

Fully qualified names are verbose. Use `@SerialName` for cleaner discriminator values:

```kotlin
import kotlinx.serialization.SerialName

sealed class Event {
    @SerialName("user.created")
    data class UserCreated(val userId: String) : Event()

    @SerialName("user.deleted")
    data class UserDeleted(val userId: String) : Event()
}

codec.slumber(Event.UserCreated("alice"))
// -> { "_type": "user.created", "userId": "alice" }
```

## Custom discriminator field

Don't want `_type`? Implement `Polymorphic.Parent` on the companion:

```kotlin
sealed class Vehicle {
    companion object : Polymorphic.Parent {
        override val discriminator = "kind"
    }

    data class Car(val seats: Int) : Vehicle() {
        companion object : Polymorphic.Child {
            override val identifier = "car"
        }
    }

    data class Bike(val gears: Int) : Vehicle() {
        companion object : Polymorphic.Child {
            override val identifier = "bike"
        }
    }
}

codec.slumber(Vehicle.Car(5))
// -> { "kind": "car", "seats": 5 }
```

## Default fallback type

When the discriminator is missing or unknown, fall back to a default type:

```kotlin
sealed class Message {
    companion object : Polymorphic.Parent {
        override val defaultType = Text::class
    }

    data class Text(val body: String) : Message()
    data class Image(val url: String) : Message()
}

// No _type field? Falls back to Text
codec.awake<Message>(mapOf("body" to "Hello!"))
// -> Message.Text(body="Hello!")
```

## Migration aliases with @AdditionalSerialName

Renamed a type? Old discriminator values still work:

```kotlin
sealed class Notification {
    @SerialName("email")
    @AdditionalSerialName("email_notification")  // old name
    @AdditionalSerialName("EmailNotification")   // even older name
    data class Email(val to: String) : Notification()
}

// All three discriminators resolve to the same class:
codec.awake<Notification>(mapOf("_type" to "email", "to" to "a@b.com"))
codec.awake<Notification>(mapOf("_type" to "email_notification", "to" to "a@b.com"))
codec.awake<Notification>(mapOf("_type" to "EmailNotification", "to" to "a@b.com"))
// -> all return Notification.Email(to="a@b.com")
```

## Polymorphic collections

Lists and maps of polymorphic types work naturally:

```kotlin
sealed class Shape {
    data class Circle(val radius: Double) : Shape()
    data class Rectangle(val width: Double, val height: Double) : Shape()
}

data class Drawing(val shapes: List<Shape>)

val drawing = Drawing(listOf(
    Shape.Circle(5.0),
    Shape.Rectangle(10.0, 20.0),
))

val data = codec.slumber(drawing)
// -> { "shapes": [
//     { "_type": "...Circle", "radius": 5.0 },
//     { "_type": "...Rectangle", "width": 10.0, "height": 20.0 }
// ]}

val restored = codec.awake<Drawing>(data)
// -> Drawing with the correct subtypes restored
```

## Nested sealed hierarchies

Sealed hierarchies can be nested — Slumber traverses them correctly:

```kotlin
sealed class Animal {
    sealed class Domestic : Animal() {
        data class Dog(val name: String) : Domestic()
        data class Cat(val name: String) : Domestic()
    }
    sealed class Wild : Animal() {
        data class Wolf(val pack: String) : Wild()
    }
}

// All leaf types serialize and deserialize correctly
codec.awake<Animal>(mapOf(
    "_type" to Animal.Domestic.Dog::class.qualifiedName,
    "name" to "Rex",
))
// -> Animal.Domestic.Dog(name="Rex")
```

---

### Collections & Maps {#slumber-collections}

# Collections & Maps

Lists, sets, maps, and nested collections — all with full type awareness.

## Lists and Sets

```kotlin
data class Team(
    val members: List<String>,
    val tags: Set<String>,
)

val codec = Codec.default

codec.slumber(Team(listOf("Alice", "Bob"), setOf("eng", "frontend")))
// -> { "members": ["Alice", "Bob"], "tags": ["eng", "frontend"] }

codec.awake<Team>(mapOf(
    "members" to listOf("Alice", "Bob"),
    "tags" to listOf("eng", "frontend"),
))
// -> Team(members=["Alice", "Bob"], tags={"eng", "frontend"})
```

Slumber supports `List`, `MutableList`, `Set`, `MutableSet`, and `Iterable`. Arrays are automatically converted to Lists
on deserialization.

## Typed collections

Inner types are fully preserved — a `List<Int>` will coerce string values to integers:

```kotlin
data class Scores(val values: List<Int>)

// String "42" is coerced to Int 42
codec.awake<Scores>(mapOf("values" to listOf("42", 100, 7)))
// -> Scores(values=[42, 100, 7])
```

## Collections of data classes

```kotlin
data class User(val name: String, val age: Int)
data class Directory(val users: List<User>)

codec.awake<Directory>(mapOf(
    "users" to listOf(
        mapOf("name" to "Alice", "age" to 30),
        mapOf("name" to "Bob", "age" to 25),
    )
))
// -> Directory(users=[User("Alice", 30), User("Bob", 25)])
```

## Maps

```kotlin
data class Config(val settings: Map<String, Int>)

codec.slumber(Config(mapOf("timeout" to 30, "retries" to 3)))
// -> { "settings": { "timeout": 30, "retries": 3 } }

codec.awake<Config>(mapOf(
    "settings" to mapOf("timeout" to 30, "retries" to 3)
))
// -> Config(settings={"timeout": 30, "retries": 3})
```

Both `Map` and `MutableMap` are supported. Key and value types are preserved through serialization.

## Nullable elements

```kotlin
data class NullableItems(val items: List<String?>)

codec.awake<NullableItems>(mapOf(
    "items" to listOf("hello", null, "world")
))
// -> NullableItems(items=["hello", null, "world"])

// Non-nullable List<String> with a null element -> error
data class StrictItems(val items: List<String>)

codec.awake<StrictItems>(mapOf(
    "items" to listOf("hello", null)
))
// -> AwakerException: "root.items.1 must not be null"
```

## Nested collections

```kotlin
data class Matrix(val rows: List<List<Int>>)

codec.awake<Matrix>(mapOf(
    "rows" to listOf(
        listOf(1, 2, 3),
        listOf(4, 5, 6),
    )
))
// -> Matrix(rows=[[1, 2, 3], [4, 5, 6]])
```

---

### Date & Time {#slumber-datetime}

# Date & Time

Slumber supports three datetime libraries out of the box: `java.time`, `kotlinx.datetime`, and Ultra's multiplatform
`MpDateTime`.

## The format

All datetime types serialize to the same structure:

```json
{
    "ts": 1649116800000,
    "timezone": "Europe/Berlin",
    "human": "2022-04-05T..."
}
```

This format stores both the precise timestamp and the timezone, so no information is lost. The `human` field is for
debugging — deserialization uses only `ts` and `timezone`.

## java.time types

```kotlin
import java.time.*

val codec = Codec.default

// Instant — absolute point in time
val instant = Instant.ofEpochMilli(1649116800000L)
codec.slumber(instant)
// -> { "ts": 1649116800000, "timezone": "UTC", "human": "2022-04-05T00:00:00Z" }

// ZonedDateTime — instant + timezone
val zdt = ZonedDateTime.ofInstant(instant, ZoneId.of("Europe/Berlin"))
codec.slumber(zdt)
// -> { "ts": 1649116800000, "timezone": "Europe/Berlin", "human": "2022-04-05T02:00+02:00[Europe/Berlin]" }

// LocalDate — date without time, stored as start-of-day UTC
val date = LocalDate.of(2022, 4, 5)
codec.slumber(date)
// -> { "ts": 1649116800000, "timezone": "UTC", "human": "2022-04-05T00:00Z[UTC]" }

// LocalDateTime — date + time without timezone, stored as UTC
val dateTime = LocalDateTime.of(2022, 4, 5, 10, 30, 0)
codec.slumber(dateTime)
// -> { "ts": 1649154600000, "timezone": "UTC", "human": "2022-04-05T10:30Z[UTC]" }

// LocalTime — time of day, stored as second-of-day
val time = LocalTime.of(14, 30, 15)
codec.slumber(time)
// -> 52215 (seconds since midnight)
```

Supported `java.time` types: `Instant`, `ZonedDateTime`, `LocalDate`, `LocalDateTime`, `LocalTime`, `ZoneId`, and
`java.util.Date`.

## kotlinx.datetime types

```kotlin
import kotlinx.datetime.*

val codec = Codec.default

// LocalDate
val date = LocalDate(2022, Month.APRIL, 5)
codec.slumber(date)
// -> { "ts": 1649116800000, "timezone": "UTC", "human": "2022-04-05T00:00:00Z" }

// LocalDateTime
val dateTime = LocalDateTime(2022, Month.APRIL, 5, 10, 30, 0)
codec.slumber(dateTime)
// -> { "ts": ..., "timezone": "UTC", "human": "..." }
```

## MpDateTime types (Ultra multiplatform)

Ultra's own multiplatform datetime types follow the same format:

```kotlin
import de.peekandpoke.ultra.common.datetime.*

val codec = Codec.default

val instant = MpLocalDate.of(2022, kotlinx.datetime.Month.APRIL, 5)
    .atStartOfDay(kotlinx.datetime.TimeZone.UTC)
    .toInstant()

codec.slumber(instant)
// -> { "ts": 1649116800000, "timezone": "UTC", "human": "2022-04-05T00:00:00.000Z" }
```

Supported MpDateTime types: `MpInstant`, `MpZonedDateTime`, `MpLocalDate`, `MpLocalDateTime`, `MpLocalTime`,
`MpTimezone`.

## DateTime in data classes

```kotlin
data class Event(
    val name: String,
    val startTime: Instant,
    val endTime: Instant?,
)

val event = Event(
    name = "Conference",
    startTime = Instant.ofEpochMilli(1649116800000L),
    endTime = null,
)

codec.slumber(event)
// -> {
//   "name": "Conference",
//   "startTime": { "ts": 1649116800000, "timezone": "UTC", "human": "..." },
//   "endTime": null
// }
```

## Timezone preservation

`ZonedDateTime` preserves the original timezone through serialization:

```kotlin
val berlin = ZonedDateTime.ofInstant(
    Instant.ofEpochMilli(1649116800000L),
    ZoneId.of("Europe/Berlin"),
)

val restored = codec.awake<ZonedDateTime>(codec.slumber(berlin))
// restored.zone == ZoneId.of("Europe/Berlin")
```

---

### Error Handling {#slumber-error-handling}

# Error Handling

Slumber's two-pass strategy gives you speed when things work and precision when they don't.

## The two-pass strategy

When you call `codec.awake()`, two things happen:

1. **Fast pass** — Deserializes with no path tracking. Minimal overhead. If it succeeds, you're done.
2. **Tracking pass** — Only runs if the fast pass throws. Re-deserializes with full path tracking, collecting diagnostic
   information at every level.

This means you pay zero cost for path tracking in the success case — which is the common case.

## Nullable vs non-nullable errors

The key design decision: **nullable fields absorb errors, non-nullable fields report them**.

```kotlin
data class Profile(val name: String, val bio: String?)

// Non-nullable field with bad data -> AwakerException
codec.awake<Profile>(mapOf("name" to listOf(1, 2, 3), "bio" to "hello"))
// -> AwakerException: "Value at path 'root.name' must not be null"

// Nullable field with bad data -> graceful null
codec.awake<Profile>(mapOf("name" to "Alice", "bio" to listOf(1, 2, 3)))
// -> Profile(name="Alice", bio=null)   <- no crash
```

## Path tracking in nested structures

Error messages include the full path to the failing field:

```kotlin
data class Address(val zip: String)
data class Company(val address: Address)
data class Employee(val company: Company)

codec.awake<Employee>(mapOf(
    "company" to mapOf(
        "address" to mapOf(
            "zip" to null  // null for non-nullable String
        )
    )
))
// -> AwakerException: "Value at path 'root.company.address.zip' must not be null"
```

## Collection index in error paths

When an error occurs inside a collection, the index is included in the path:

```kotlin
data class Container(val items: List<Int>)

codec.awake<Container>(mapOf(
    "items" to listOf(1, null, 3)
))
// -> AwakerException: "Value at path 'root.items.1' must not be null"
//                                              ^ index 1
```

## Missing required parameters

When a non-nullable, non-default parameter is missing from the input:

```kotlin
data class User(val name: String, val age: Int)

codec.awake<User>(mapOf("name" to "Alice"))
// -> AwakerException with message containing:
//   "Value at path 'root' must not be null"
//   "'root': User misses parameters 'age'"
```

## Exception types

Slumber defines three exception classes:

```kotlin
// Base class for all Slumber errors
open class SlumberException(message: String) : Throwable(message)

// Thrown when deserialization fails
class AwakerException(
    message: String,
    val logs: List<String>,     // Diagnostic messages from nested levels
    val rootType: KType?,       // The type being deserialized
    val input: Any?,            // The input data that caused the error
) : SlumberException(message)

// Thrown when serialization fails (e.g. null for non-nullable type)
class SlumbererException(
    message: String,
    val input: Any?,            // The data that caused the error
) : SlumberException(message)
```

## Catching and handling errors

```kotlin
try {
    val user = codec.awake<User>(untrustedData)
} catch (e: AwakerException) {
    println("Deserialization failed at: extracted from message")
    println("Root type: ${e.rootType}")
    println("Input: ${e.input}")
    println("Diagnostics: ${e.logs.joinToString("\n")}")
}
```

## Default parameter fallback

When a field has a default value and the input provides invalid data, the default is used:

```kotlin
data class Config(
    val host: String = "localhost",
    val port: Int = 8080,
)

// Invalid data for "port" -> falls back to default
codec.awake<Config>(mapOf("host" to "myserver", "port" to "not_a_number"))
// -> Config(host="myserver", port=8080)
```

---

### Custom Modules {#slumber-custom-modules}

# Custom Modules

Teach Slumber to handle your own types by implementing `SlumberModule`. No annotation processors — just an interface.

## How modules work

A `SlumberConfig` holds an ordered list of `SlumberModule`s. When Slumber needs to serialize or deserialize a type, it
asks each module in order. The first module to return a non-null handler wins.

```kotlin
interface SlumberModule {
    fun getAwaker(type: KType, attributes: TypedAttributes): Awaker?
    fun getSlumberer(type: KType, attributes: TypedAttributes): Slumberer?
}
```

Return `null` to pass — "I don't handle this type." Return an `Awaker` or `Slumberer` to claim it.

## Example: Custom Money type

```kotlin
data class Money(val amount: Long, val currency: String)

object MoneyModule : SlumberModule {

    override fun getAwaker(type: KType, attributes: TypedAttributes): Awaker? {
        if (type.classifier != Money::class) return null

        return type.wrapIfNonNull(object : Awaker {
            override fun awake(data: Any?, context: Awaker.Context): Money? {
                val map = data as? Map<*, *> ?: return null
                val amount = (map["amount"] as? Number)?.toLong() ?: return null
                val currency = map["currency"] as? String ?: return null
                return Money(amount, currency)
            }
        })
    }

    override fun getSlumberer(type: KType, attributes: TypedAttributes): Slumberer? {
        if (type.classifier != Money::class) return null

        return type.wrapIfNonNull(object : Slumberer {
            override fun slumber(data: Any?, context: Slumberer.Context): Any? {
                val money = data as? Money ?: return null
                return mapOf("amount" to money.amount, "currency" to money.currency)
            }
        })
    }
}
```

## Registering a custom module

```kotlin
// Prepend to give your module highest priority
val config = SlumberConfig.default.prependModules(MoneyModule)
val codec = config.codec()

// Now Money is handled by your module
codec.slumber(Money(1099, "EUR"))
// -> { "amount": 1099, "currency": "EUR" }

codec.awake<Money>(mapOf("amount" to 1099, "currency" to "EUR"))
// -> Money(amount=1099, currency="EUR")
```

## wrapIfNonNull

Notice the `type.wrapIfNonNull(awaker)` call. This is a helper on `SlumberModule` that wraps your awaker in a
`NonNullAwaker` when the type is non-nullable. This ensures that:

- `Money` (non-nullable) — throws `AwakerException` if your awaker returns null
- `Money?` (nullable) — returns null gracefully

Always use `wrapIfNonNull` unless you have a specific reason not to.

## Module ordering

Modules are queried in order. Use `prependModules` when your module should override built-in behavior, and
`appendModules` when it's a fallback:

```kotlin
// Override: your module takes priority over built-in handlers
val config = SlumberConfig.default.prependModules(MoneyModule)

// Fallback: your module only handles types the built-in modules don't
val config = SlumberConfig.default.appendModules(FallbackModule)
```

## Passing context via attributes

Modules receive `TypedAttributes` — a typed key-value store. Use this to pass runtime context:

```kotlin
// Define a typed key
val DatabaseKey = TypedKey<Database>("database")

// Add it to the config
val config = SlumberConfig.default
    .prependModules(MyDbModule)
    .plusAttributes(TypedAttributes.of { add(DatabaseKey, myDatabase) })

// Access it in your module
object MyDbModule : SlumberModule {
    override fun getAwaker(type: KType, attributes: TypedAttributes): Awaker? {
        val db = attributes[DatabaseKey] // Access the database
        // ... use db to resolve references
    }
}
```

This is how the Vault and Karango integrations work — they pass `Database` and `EntityCache` references through
attributes so their custom modules can resolve entity references during serialization.

## The default module stack

`SlumberConfig.default` includes these modules in order:

1. **MpDateTimeModule** — Ultra's multiplatform datetime types
2. **KotlinxTimeModule** — kotlinx.datetime types
3. **JavaTimeModule** — java.time types
4. **BuiltInModule** — primitives, strings, collections, maps, data classes, enums, sealed classes, singletons

DateTime modules come first because they handle specific types that the BuiltInModule would otherwise catch as generic
data classes.

---
