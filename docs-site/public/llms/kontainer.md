## Kontainer — Dependency Injection

### Overview {#kontainer-overview}

# Kontainer — Dependency Injection, Done Right

A lightweight DI container for Kotlin. Constructor injection, lifecycle management,
and compile-time verification — no magic, no annotations, no surprises.

## What is Kontainer?

Kontainer is a dependency injection container that uses a **two-stage architecture**: you define a blueprint, then
create container instances from it. Services are injected via constructors — no annotations, no reflection tricks, no
runtime surprises.

```kotlin
val blueprint = kontainer {
    singleton(UserRepository::class)
    singleton(UserService::class)
    dynamic(RequestContext::class)
}

val kontainer = blueprint.create()
val service = kontainer.get(UserService::class)
```

That's it. Define services, create a container, get what you need.

## The core idea: stateful, scoped containers

Most DI containers give you a single, long-lived container. Kontainer is different — it's built around the idea that *
*you create a fresh container for every unit of work**.

On the server side, every incoming HTTP request, CLI command, or scheduled job gets its own container instance, created
from a shared blueprint. Singletons (database pools, configuration) are shared. But **dynamic services are fresh per
container** — they can carry state that lives exactly as long as the request does, then gets garbage collected. Clean
slate, every time.

This unlocks powerful patterns:

- **Inject the current user everywhere** — set it once when the request starts, every service that needs it gets it via
  constructor injection. No parameter threading.
- **Per-request metrics and audit logs** — collect database query counts, timings, and log entries throughout a request.
  Flush at the end. No manual cleanup.
- **Request-scoped caching** — cache expensive lookups for the duration of one request without worrying about stale data
  across requests.

Think of it as PHP's "shared nothing" model, but selective — you choose which services are global and which are
per-request.

## Why Kontainer?

**Not another Dagger clone.** Kontainer doesn't generate code at compile time or scan classpath at runtime. Instead, it
validates the entire dependency graph on first use and gives you clear error messages when something doesn't wire up.

- **Pure Kotlin DSL** — no annotations, no XML, no YAML. Your DI config is code.
- **Three lifecycles** — Singleton (global), Dynamic (per-container), Prototype (per-injection). Choose what fits.
- **Validated on first use** — missing dependencies, ambiguous services, and inconsistencies are caught immediately with
  clear messages.
- **Constructor injection** — services declare what they need in their constructor. Kontainer fills it in.
- **No classpath scanning** — you control exactly what goes into the container.

## What you get

- **Service Lifecycles** — Singleton, Dynamic, and Prototype — each with clear, predictable behavior.
- **Constructor Injection** — Declare dependencies in your constructor. Kontainer resolves them automatically.
- **Factory Methods** — When constructors aren't enough, use factory lambdas with up to 10 injected parameters.
- **Lazy & List Injection** — `Lazy<T>`, `List<T>`, `Lookup<T>` — inject one, many, or defer.
- **Modules** — Group services into reusable modules. Libraries ship a module, apps include it.
- **Debug Tools** — Code location tracking, container dumps, and debug info for every service.

## Quick taste

A service with dependencies, resolved automatically:

```kotlin
class UserRepository {
    fun findById(id: String) = "User #$id"
}

class UserService(private val repo: UserRepository) {
    fun greet(id: String) = "Hello, ${repo.findById(id)}!"
}

val blueprint = kontainer {
    singleton(UserRepository::class)
    singleton(UserService::class)
}

val kontainer = blueprint.create()
println(kontainer.get(UserService::class).greet("42"))
// "Hello, User #42!"
```

---

### Getting Started {#kontainer-getting-started}

# Getting Started

Add Kontainer to your project and wire up your first services.

## Prerequisites

- **JDK 17+**
- **Gradle** with Kotlin JVM or multiplatform plugin

## 1. Add the dependency

In your `build.gradle.kts`:

```kotlin
dependencies {
    implementation("io.peekandpoke.ultra:kontainer:0.102.0")
}
```

Find the latest version on [Maven Central](https://central.sonatype.com/namespace/io.peekandpoke.ultra).

## 2. Define a blueprint

A **blueprint** is an immutable description of all your services and their lifecycles. You create it once, then use it
to stamp out container instances.

```kotlin
import io.peekandpoke.ultra.kontainer.kontainer

// Define some services
class Greeter {
    fun sayHello() = "Hello!"
}

// Create a blueprint
val blueprint = kontainer {
    singleton(Greeter::class)
}
```

## 3. Create a container and use it

```kotlin
// Create a container from the blueprint
val kontainer = blueprint.create()

// Retrieve a service
val greeter = kontainer.get(Greeter::class)
println(greeter.sayHello())  // "Hello!"
```

## 4. Add dependencies between services

Services declare their dependencies in the constructor. Kontainer resolves them automatically.

```kotlin
class Counter {
    private var count = 0
    fun next() = ++count
}

class MyService(val counter: Counter)

val blueprint = kontainer {
    singleton(Counter::class)
    singleton(MyService::class)
}

val kontainer = blueprint.create()
val service = kontainer.get(MyService::class)

println(service.counter.next())  // 1
println(service.counter.next())  // 2
```

Kontainer inspects the primary constructor of `MyService`, sees it needs a `Counter`, and injects it. No annotations
needed.

## 5. Use factory methods when constructors aren't enough

Sometimes a service needs parameters the container doesn't know about. Use a factory lambda:

```kotlin
class MyService(private val counter: Counter, private val offset: Int) {
    fun next() = counter.next() + offset
}

val blueprint = kontainer {
    singleton(Counter::class)

    // Factory: Kontainer injects Counter, you provide the offset
    singleton(MyService::class) { counter: Counter ->
        MyService(counter, 100)
    }
}
```

## What happens on first use?

The first time you call `blueprint.create()`, Kontainer validates the entire dependency graph:

- Are all required dependencies registered?
- Are there ambiguous services (multiple implementations for one type)?
- Can all constructors be satisfied?

If anything is wrong, you get a `KontainerInconsistent` exception with a clear message listing every problem — not a
cryptic runtime error later.

---

### Defining Services {#kontainer-services}

# Defining Services

Kontainer supports three service lifecycles — Singleton, Dynamic, and Prototype — plus existing instances and
interface-based registration.

## Singleton

A singleton is created **once** and shared across all container instances created from the same blueprint.

```kotlin
class Counter {
    private var count = 0
    fun next() = ++count
}

val blueprint = kontainer {
    singleton(Counter::class)
}

// Same instance across all containers from this blueprint
val k1 = blueprint.create()
val k2 = blueprint.create()

println(k1.get(Counter::class).next())  // 1
println(k2.get(Counter::class).next())  // 2  (same instance!)
```

## Dynamic

A dynamic service is a **singleton within a single container instance**. Each new container gets its own fresh instance.

```kotlin
val blueprint = kontainer {
    dynamic(Counter::class)
}

val k1 = blueprint.create()
val k2 = blueprint.create()

println(k1.get(Counter::class).next())  // 1
println(k1.get(Counter::class).next())  // 2  (same instance within k1)
println(k2.get(Counter::class).next())  // 1  (fresh instance in k2)
```

Dynamic services are the key to per-request scoping.

## Prototype

A prototype creates a **new instance every time** it is requested — whether retrieved directly or injected.

```kotlin
val blueprint = kontainer {
    prototype(Counter::class)
}

val kontainer = blueprint.create()

println(kontainer.get(Counter::class).next())  // 1
println(kontainer.get(Counter::class).next())  // 1  (new instance each time!)
```

## Existing instances

Register an already-created object as a singleton:

```kotlin
object AppConfig {
    val dbUrl = "jdbc:postgresql://localhost/mydb"
}

val blueprint = kontainer {
    instance(AppConfig)
}
```

## Hiding implementations behind interfaces

Register a service under its interface so consumers never see the concrete class:

```kotlin
interface GreeterInterface {
    fun sayHello(): String
}

class Greeter : GreeterInterface {
    override fun sayHello() = "Hello!"
}

val blueprint = kontainer {
    // Registered as GreeterInterface — Greeter is hidden
    singleton(GreeterInterface::class, Greeter::class)
}

val kontainer = blueprint.create()

kontainer.get(GreeterInterface::class)  // works
kontainer.get(Greeter::class)           // throws ServiceNotFound
```

This pattern works with all lifecycle types: `singleton`, `dynamic`, `prototype`, `instance`, and factory methods.

---

### Service Lifecycle {#kontainer-lifecycle}

# Service Lifecycle

How Kontainer's lifecycle model enables per-request scoping, stateful dynamic services, and automatic cleanup — without
manual wiring.

## Per-request containers

The real power of Kontainer's lifecycle model shows up on the server side. The pattern is simple and inspired by PHP's
request model:

1. Define a **blueprint once** at application startup
2. For every incoming request (or CLI command, scheduled job, etc.), **create a fresh container** from the blueprint
3. Dynamic services carry **per-request state** — the current user, collected metrics, audit context
4. When the request is done, the container and all its dynamic services are **garbage collected** — clean slate, no
   leaks

Singletons survive across requests (database pools, caches, configuration). Dynamic services are scoped to exactly one
unit of work. This gives you the simplicity of "everything is fresh per request" without the cost of recreating
stateless services.

```kotlin
// Define once at app startup
val blueprint = kontainer {
    // Shared across all requests
    singleton(DatabasePool::class)
    singleton(UserRepository::class)
    singleton(AuditLogRepository::class)

    // Fresh per request — carry request-scoped state
    dynamic(RequestContext::class)
    dynamic(RequestInsights::class)
    dynamic(AuditLog::class)
}

// On each incoming request:
val kontainer = blueprint.create {
    with(RequestContext::class) {
        RequestContext(userId = authenticatedUser.id, traceId = request.traceId)
    }
}
```

### Example: Injecting the current user everywhere

Since `RequestContext` is a dynamic service, any service can inject it and access the current user — without passing it
through every method call:

```kotlin
class RequestContext(val userId: String, val traceId: String)

class AuditLog(private val ctx: RequestContext) {
    private val entries = mutableListOf<String>()

    fun record(action: String) {
        entries.add("[${ctx.traceId}] User ${ctx.userId}: $action")
    }

    fun getEntries() = entries.toList()
}

class OrderService(
    private val ctx: RequestContext,
    private val auditLog: AuditLog,
    private val repo: OrderRepository,
) {
    fun placeOrder(item: String) {
        repo.save(Order(item, ctx.userId))
        auditLog.record("placed order for $item")
    }
}
```

The `AuditLog` and `OrderService` both get the same `RequestContext` instance — scoped to this request. Next request,
fresh instances.

### Example: Per-request metrics collection

Collect metrics throughout request processing, then flush them at the end. This pattern is used extensively in **Funktor
Insights**:

```kotlin
class RequestInsights {
    private val dbQueries = mutableListOf<QueryRecord>()
    private val logs = mutableListOf<LogEntry>()
    private val timings = mutableMapOf<String, Long>()

    fun recordDbQuery(query: String, durationMs: Long) {
        dbQueries.add(QueryRecord(query, durationMs))
    }

    fun recordLog(level: String, message: String) {
        logs.add(LogEntry(level, message))
    }

    fun recordTiming(label: String, durationMs: Long) {
        timings[label] = durationMs
    }

    fun summary() = InsightsSummary(
        totalDbQueries = dbQueries.size,
        totalDbTimeMs = dbQueries.sumOf { it.durationMs },
        logs = logs.toList(),
        timings = timings.toMap(),
    )
}

// Any service in the request can record metrics
class UserRepository(private val insights: RequestInsights) {
    fun findById(id: String): User {
        val start = System.currentTimeMillis()
        val result = db.query("SELECT * FROM users WHERE id = ?", id)
        insights.recordDbQuery("findById", System.currentTimeMillis() - start)
        return result
    }
}

// At the end of the request, flush the collected insights
val summary = kontainer.get(RequestInsights::class).summary()
metricsService.report(summary)
```

Because `RequestInsights` is dynamic, every service in the request writes to the same instance. When the request ends
and the container is garbage collected, all that state goes with it — no manual cleanup needed.

## Lifecycles side by side

The best way to understand the three lifecycles is to see them running together:

```kotlin
abstract class Counter {
    private var count = 0
    fun next() = ++count
}

class SingletonCounter : Counter()
class DynamicCounter : Counter()
class PrototypeCounter : Counter()

val blueprint = kontainer {
    singleton(SingletonCounter::class)
    dynamic(DynamicCounter::class)
    prototype(PrototypeCounter::class)
}

for (round in 1..3) {
    println("Round #$round")
    val kontainer = blueprint.create()

    repeat(3) {
        val s = kontainer.get(SingletonCounter::class).next()
        val d = kontainer.get(DynamicCounter::class).next()
        val p = kontainer.get(PrototypeCounter::class).next()
        println("  singleton: $s  dynamic: $d  prototype: $p")
    }
}
```

Output:

| Container | Call | singleton | dynamic | prototype |
|-----------|------|-----------|---------|-----------|
| Round #1  | 1st  | 1         | 1       | 1         |
|           | 2nd  | 2         | 2       | 1         |
|           | 3rd  | 3         | 3       | 1         |
| Round #2  | 1st  | 4         | 1       | 1         |
|           | 2nd  | 5         | 2       | 1         |
|           | 3rd  | 6         | 3       | 1         |
| Round #3  | 1st  | 7         | 1       | 1         |
|           | 2nd  | 8         | 2       | 1         |
|           | 3rd  | 9         | 3       | 1         |

Notice the pattern:

- **Singleton** keeps counting globally (1 to 9) — same instance across all three containers
- **Dynamic** resets to 1 each round — fresh instance per container, but stable within it
- **Prototype** is always 1 — brand new instance on every `.get()` call

## Automatic lifecycle promotion (semi-dynamic)

There's a subtle but important problem with mixing singletons and dynamic services. Consider this:

```kotlin
class RequestContext(val userId: String)   // dynamic — fresh per request

class OrderService(val ctx: RequestContext)  // singleton — shared globally?
```

If `OrderService` were a true singleton, it would be created once and hold a reference to the `RequestContext` from the
*first* request forever. Every subsequent request would see the wrong user. That's a bug.

Kontainer prevents this automatically. When a service is defined as `singleton` but injects a `dynamic` service (
directly or transitively), Kontainer **promotes** it to **semi-dynamic** — it behaves like a dynamic service, getting a
fresh instance per container.

```kotlin
val blueprint = kontainer {
    dynamic(RequestContext::class)

    // Defined as singleton, but Kontainer detects that it injects
    // a dynamic service and promotes it to semi-dynamic automatically.
    singleton(OrderService::class)
    singleton(AuditLog::class)
}
```

You don't need to do anything — Kontainer analyzes the dependency graph and makes the right call. The rule is simple:

**If you depend on something that must be cleaned up per unit of work, you must be cleaned up too.**

This promotion **bubbles up the dependency tree**. If service A injects service B, and B injects a dynamic service C,
then both A and B become semi-dynamic — even though only C was explicitly marked as dynamic.

```kotlin
class RequestContext(val userId: String)        // dynamic

class AuditLog(val ctx: RequestContext)           // singleton -> semi-dynamic (injects dynamic)

class OrderService(val audit: AuditLog)           // singleton -> semi-dynamic (injects semi-dynamic)

class OrderController(val orders: OrderService)   // singleton -> semi-dynamic (injects semi-dynamic)
```

The only services that remain true singletons are those with **no transitive dependency** on any dynamic service — like
a database pool or a configuration object. Everything else is automatically scoped correctly.

## Comparison

| Lifecycle      | Created                 | Shared across containers | Use case                                              |
|----------------|-------------------------|--------------------------|-------------------------------------------------------|
| `singleton`    | Once, globally          | Yes                      | Stateless services, DB pools, configs                 |
| `dynamic`      | Once per container      | No                       | Request context, user, audit logs, metrics            |
| `prototype`    | Every injection / get() | No                       | Disposable, short-lived objects                       |
| *semi-dynamic* | Once per container      | No                       | Auto-promoted singletons that inject dynamic services |

---

### Service Injection {#kontainer-injection}

# Service Injection

Kontainer supports constructor injection, factory methods, lazy injection, and injecting multiple implementations of an
interface.

## Constructor injection

The simplest and most common pattern. Declare dependencies in your constructor:

```kotlin
class Counter {
    private var count = 0
    fun next() = ++count
}

class MyService(val counter: Counter)

val blueprint = kontainer {
    singleton(Counter::class)
    singleton(MyService::class)
}

val kontainer = blueprint.create()
val service = kontainer.get(MyService::class)

println(service.counter.next())  // 1
println(service.counter.next())  // 2
```

## Factory method injection

When a service needs parameters that Kontainer doesn't know about, use a factory lambda. The lambda's parameters are
injected; you handle the rest.

```kotlin
class MyService(private val counter: Counter, private val offset: Int) {
    fun next() = counter.next() + offset
}

val blueprint = kontainer {
    singleton(Counter::class)

    // Kontainer injects Counter; you provide the offset
    singleton(MyService::class) { counter: Counter ->
        MyService(counter, 100)
    }
}
```

Factories support up to 10 injected parameters.

## Inject by super type

A service can be injected by any of its supertypes — interface or parent class:

```kotlin
interface CounterInterface {
    fun next(): Int
}

class Counter : CounterInterface {
    private var count = 0
    override fun next() = ++count
}

// MyService depends on the interface, not the concrete class
class MyService(val counter: CounterInterface)

val blueprint = kontainer {
    singleton(Counter::class)
    singleton(MyService::class)
}
```

If two services implement the same interface, injection becomes **ambiguous** and Kontainer reports an error. Use
`List<T>` injection instead (see below).

## Nullable injection

Mark a dependency as nullable to make it optional. If the service isn't registered, `null` is injected instead of
throwing:

```kotlin
class SomeOptionalService

// Will receive null if SomeOptionalService is not registered
class MyService(val optional: SomeOptionalService?)

val blueprint = kontainer {
    singleton(MyService::class)
    // SomeOptionalService is NOT registered
}

val kontainer = blueprint.create()
println(kontainer.get(MyService::class).optional)  // null
```

## Inject all implementations

Inject all services that implement a given type as a `List`. This is powerful for building extensible, plugin-style
architectures:

```kotlin
interface Repository {
    val name: String
}

class UserRepository : Repository {
    override val name = "users"
}

class OrderRepository : Repository {
    override val name = "orders"
}

// Injects ALL Repository implementations
class Database(val repos: List<Repository>)

val blueprint = kontainer {
    singleton(Database::class)
    singleton(UserRepository::class)
    singleton(OrderRepository::class)
}

val kontainer = blueprint.create()
kontainer.get(Database::class).repos.forEach {
    println(it.name)  // "users", "orders"
}
```

## Lazy injection

Wrap a dependency in `Lazy<T>` to defer its creation until first use:

```kotlin
class ExpensiveService {
    init { println("ExpensiveService created!") }
    fun doWork() = "done"
}

class MyService(private val lazy: Lazy<ExpensiveService>) {
    fun run() = lazy.value.doWork()
}

val blueprint = kontainer {
    singleton(ExpensiveService::class)
    singleton(MyService::class)
}

val kontainer = blueprint.create()
val service = kontainer.get(MyService::class)
// ExpensiveService is NOT created yet

service.run()
// NOW ExpensiveService is created
```

## Breaking circular dependencies with Lazy

When two services need each other, make one of them lazy to break the cycle:

```kotlin
class ServiceOne(private val two: ServiceTwo) {
    val name = "one"
    fun greet() = "I know ${two.name}"
}

class ServiceTwo(private val one: Lazy<ServiceOne>) {
    val name = "two"
    fun greet() = "I know ${one.value.name}"
}

val blueprint = kontainer {
    singleton(ServiceOne::class)
    singleton(ServiceTwo::class)
}

val kontainer = blueprint.create()
println(kontainer.get(ServiceOne::class).greet())  // "I know two"
println(kontainer.get(ServiceTwo::class).greet())  // "I know one"
```

## Lookup injection

For large sets of implementations where you only need specific ones at a time, use `Lookup<T>`. Unlike `List<T>`, a
Lookup creates services on demand:

```kotlin
import io.peekandpoke.ultra.common.Lookup

class Database(val repos: Lookup<Repository>)

val blueprint = kontainer {
    singleton(Database::class)
    dynamic(UserRepository::class)
    dynamic(OrderRepository::class)
}

val kontainer = blueprint.create()
val db = kontainer.get(Database::class)

// Only UserRepository is created — OrderRepository stays untouched
val users = db.repos.get(UserRepository::class)
println(users.name)  // "users"
```

## Injection patterns summary

| Pattern   | Constructor type          | Behavior                   |
|-----------|---------------------------|----------------------------|
| Direct    | `val x: Service`          | Injected immediately       |
| Nullable  | `val x: Service?`         | Null if not registered     |
| List      | `val x: List<Base>`       | All implementations        |
| Lookup    | `val x: Lookup<Base>`     | Lazy, on-demand access     |
| Lazy      | `val x: Lazy<Service>`    | Created on first `.value`  |
| Lazy List | `val x: Lazy<List<Base>>` | Deferred list of all impls |

---

### Modules {#kontainer-modules}

# Modules

Group related services into reusable modules. Libraries ship a module, applications include it — with optional
overrides.

## What are modules?

A Kontainer module is a reusable block of service definitions. Instead of copying service registrations across projects,
you define them once in a module and include it.

Modules are useful for:

- **Library authors** — bundle your library's services into a module users can drop in
- **Large applications** — organize services by feature area
- **Customization** — provide sensible defaults that users can override

## Defining a simple module

```kotlin
import io.peekandpoke.ultra.kontainer.module

// Some services
interface Storage {
    val name: String
}

class FileStorage : Storage {
    override val name = "FileStorage"
}

class Database(val storage: Storage)

// Define the module
val databaseModule = module {
    singleton(Database::class)
    singleton(Storage::class, FileStorage::class)
}
```

## Using a module

```kotlin
val blueprint = kontainer {
    module(databaseModule)
}

val kontainer = blueprint.create()
println(kontainer.get(Database::class).storage.name)  // "FileStorage"
```

## Overriding module services

Any service defined by a module can be overridden. Just register the same type after including the module:

```kotlin
class MemoryStorage : Storage {
    override val name = "MemoryStorage"
}

val blueprint = kontainer {
    module(databaseModule)

    // Override the Storage implementation
    singleton(Storage::class, MemoryStorage::class)
}

val kontainer = blueprint.create()
println(kontainer.get(Database::class).storage.name)  // "MemoryStorage"
```

## Parameterized modules

Modules can accept parameters — up to 5 — for configuration at inclusion time:

```kotlin
class Service(val sum: Int)

// Module with three parameters
val configModule = module { a: Int, b: Int, c: Int ->
    instance(Service(a + b + c))
}

val blueprint = kontainer {
    module(configModule, 1, 10, 100)
}

val kontainer = blueprint.create()
println(kontainer.get(Service::class).sum)  // 111
```

## Composing modules

Modules can include other modules, enabling layered architectures:

```kotlin
val coreModule = module {
    singleton(Counter::class)
}

val appModule = module {
    module(coreModule)
    singleton(MyService::class)
}

val blueprint = kontainer {
    module(appModule)
}
```

---

### Advanced Topics {#kontainer-advanced}

# Advanced Topics

Dynamic overrides, blueprint extension, injection context, semi-dynamic services, and debugging tools.

## Dynamic overrides

When creating a container from a blueprint, you can override dynamic services with specific instances. This is essential
for request-scoped data in web applications:

```kotlin
class RequestContext(val userId: String)

class UserService(val ctx: RequestContext) {
    fun currentUser() = "User: ${ctx.userId}"
}

val blueprint = kontainer {
    dynamic(RequestContext::class)
    singleton(UserService::class)
}

// Override the dynamic service when creating a container
val kontainer = blueprint.create {
    with(RequestContext::class) { RequestContext("user-42") }
}

println(kontainer.get(UserService::class).currentUser())
// "User: user-42"
```

## Semi-dynamic services

If a singleton injects a dynamic service, Kontainer automatically promotes it to **semi-dynamic** — it becomes a
singleton within each container instance rather than global. This happens transparently; you don't need to do anything.

```kotlin
class RequestContext(val userId: String)

// UserService is defined as singleton, but it injects a dynamic service.
// Kontainer promotes it to semi-dynamic automatically.
class UserService(val ctx: RequestContext)

val blueprint = kontainer {
    dynamic(RequestContext::class)
    singleton(UserService::class)  // automatically becomes semi-dynamic
}
```

## Blueprint extension

Create a new blueprint by extending an existing one. The original is not modified:

```kotlin
val base = kontainer {
    singleton(Counter::class)
}

val extended = base.extend {
    singleton(MyService::class)
}

// 'extended' has both Counter and MyService
// 'base' still only has Counter
```

## Cloning containers

Clone a container to get a fresh instance with reset dynamic services. Optionally apply new overrides:

```kotlin
val kontainer = blueprint.create {
    with(RequestContext::class) { RequestContext("user-1") }
}

// Clone with different dynamic overrides
val cloned = kontainer.clone {
    with(RequestContext::class) { RequestContext("user-2") }
}
```

## Injecting the container and blueprint

The `Kontainer` itself and the `KontainerBlueprint` are automatically available as services. Any service can inject
them:

```kotlin
class ServiceLocator(val kontainer: Kontainer) {
    fun <T : Any> resolve(cls: KClass<T>): T = kontainer.get(cls)
}

class BlueprintAware(val blueprint: KontainerBlueprint) {
    fun createChild() = blueprint.create()
}

val blueprint = kontainer {
    singleton(ServiceLocator::class)
    singleton(BlueprintAware::class)
}
```

## Injection context

Services can inject `InjectionContext` to know who is requesting them and what is being injected. Useful for logging and
context-aware behavior:

```kotlin
class ContextAwareService(val ctx: InjectionContext) {
    fun whoAsked() = ctx.requestingClass.simpleName
}
```

## Retrieving services

Kontainer provides several ways to get services:

```kotlin
val kontainer = blueprint.create()

// Get — throws if not found
val service = kontainer.get(MyService::class)

// Get with reified type
val service2 = kontainer.get<MyService>()

// GetOrNull — returns null if not found
val maybe = kontainer.getOrNull(MyService::class)

// Use — runs block only if service exists
kontainer.use(MyService::class) {
    println(this.doSomething())
}

// GetAll — all implementations of a type
val allRepos: List<Repository> = kontainer.getAll(Repository::class)

// GetLookup — lazy lookup of implementations
val lookup: LazyServiceLookup<Repository> = kontainer.getLookup(Repository::class)
```

## Debug tools

Every container comes with debugging tools:

```kotlin
val kontainer = blueprint.create()

// Dump all services, their types, and instances
println(kontainer.tools.dumpKontainer())

// Get structured debug info
val debugInfo = kontainer.tools.getDebugInfo()
debugInfo.services.forEach { service ->
    println("${service.cls.fqn} [type=${service.type}]")
    service.definition.injects.forEach { param ->
        println("  -> ${param.name}: ${param.classes.map { it.fqn }}")
    }
}
```

## Error handling

Kontainer throws specific exceptions for different problems:

| Exception               | When                                                               |
|-------------------------|--------------------------------------------------------------------|
| `KontainerInconsistent` | Dependency graph has problems (missing deps, configuration errors) |
| `ServiceNotFound`       | Requested service is not registered                                |
| `ServiceAmbiguous`      | Multiple services match the requested type                         |
| `InvalidClassProvided`  | Tried to register an interface or abstract class directly          |
| `InvalidServiceFactory` | Factory method is invalid                                          |

---
