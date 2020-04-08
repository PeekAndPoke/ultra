# Examples for ultra::kontainer

## TOC

1. [Defining Services](#defining-services)

    1. [Defining a Singleton service](#defining-a-singleton-service)
    2. [Defining a Dynamic service](#defining-a-dynamic-service)
    3. [Defining a Prototype service](#defining-a-prototype-service)
    4. [Defining an existing object as a Singleton service](#defining-an-existing-object-as-a-singleton-service)
    5. [Singleton are shared](#singleton-are-shared)
    6. [Singletons vs Dynamics vs Prototypes](#singletons-vs-dynamics-vs-prototypes)

2. [Service Injection](#service-injection)

    1. [Basic Injection Example](#basic-injection-example)
    2. [Factory Method Injection](#factory-method-injection)
    3. [Injecting a singleton into multiple services](#injecting-a-singleton-into-multiple-services)
    4. [Injecting a prototype into multiple services](#injecting-a-prototype-into-multiple-services)
    5. [Injection By SuperType](#injection-by-supertype)
    6. [Injection of all Services By SuperType](#injection-of-all-services-by-supertype)
    7. [Lazy Injection Example](#lazy-injection-example)
    8. [Breaking Cyclic Dependencies with Lazy Injection](#breaking-cyclic-dependencies-with-lazy-injection)
    9. [Lazily inject all Services By SuperType](#lazily-inject-all-services-by-supertype)
    10. [Lazily inject all Services By SuperType with a Lookup](#lazily-inject-all-services-by-supertype-with-a-lookup)

## Defining Services

### Defining a Singleton service

This example shows how to register and retrieve a simple singleton service.

Singleton services are created only once. They are shared across all kontainer instances that
are created from the same kontainer blueprint.

Services can be retrieved by:
1. kontainer.get(...)
2. kontainer.use(...)

(see the full [example](../../src/examples/_01_defining_services/DefiningASingletonExample.kt))

```kotlin
// 1. We define a service class
class Greeter {
    fun sayHello() = "Hello you!"
}

// 2. We create a kontainer blueprint
val blueprint = kontainer {
    singleton(Greeter::class)
}

// 3. We get the kontainer instance
val kontainer = blueprint.create()

// 4. We can retrieve our service by kontainer.use(...).
//    And we will have the service as "this" inside of the closure.
//
// NOTICE: If the service was not present in the kontainer, the closure would not be executed.
//         This is useful, when we only want to execute some code, when a service exists.
kontainer.use(Greeter::class) {
    println("Kontainer.use() says: ${sayHello()}")
}

// 5. We can also retrieve the service by kontainer.get(...)
//
// NOTICE: If the service was not present in the kontainer, an exception would be thrown.
println(
    "Kontainer.get() says: ${kontainer.get(Greeter::class).sayHello()}"
)
```
Will output:
```
Kontainer.use() says: Hello you!
Kontainer.get() says: Hello you!
```
### Defining a Dynamic service

This example shows how to register and retrieve a dynamic service.

Dynamic services are somewhere between singletons and prototypes.  
They are instantiated once for each kontainer instances.

(see the full [example](../../src/examples/_01_defining_services/DefiningADynamicExample.kt))

```kotlin
// 1. We define a service class
class Greeter {
    fun sayHello() = "Hello you!"
}

// 2. We create a kontainer blueprint
val blueprint = kontainer {
    dynamic(Greeter::class)
}

// 3. We get the kontainer instance
val kontainer = blueprint.create()

// 4. We can retrieve our service by kontainer.use(...).
//    And we will have the service as "this" inside of the closure.
kontainer.use(Greeter::class) {
    println("Kontainer.use() says: ${sayHello()}")
}
```
Will output:
```
Kontainer.use() says: Hello you!
```
### Defining a Prototype service

This example shows how to register and retrieve a prototype service.

Prototype services create a new instance whenever they are requested from the kontainer.  
Be it through direct retrieval from the kontainer or through injection.

(see the full [example](../../src/examples/_01_defining_services/DefiningAPrototypeExample.kt))

```kotlin
// 1. We define a service class
class Greeter {
    fun sayHello() = "Hello you!"
}

// 2. We create a kontainer blueprint
val blueprint = kontainer {
    prototype(Greeter::class)
}

// 3. We get the kontainer instance
val kontainer = blueprint.create()

// 4. We can retrieve our service by kontainer.use(...).
//    And we will have the service as "this" inside of the closure.
kontainer.use(Greeter::class) {
    println("Kontainer.use() says: ${sayHello()}")
}
```
Will output:
```
Kontainer.use() says: Hello you!
```
### Defining an existing object as a Singleton service

This example shows how to register an existing object as a singleton service.

(see the full [example](../../src/examples/_01_defining_services/DefiningAnExistingInstanceExample.kt))

```kotlin
// 1. Let's say we have some existing object
object Greeter {
    fun sayHello() = "Hello you!"
}

// 2. We create a kontainer blueprint
val blueprint = kontainer {
    instance(Greeter)
}

// 3. We get the kontainer instance
val kontainer = blueprint.create()

// 4. We can retrieve our service by kontainer.use(...).
//    And we will have the service as "this" inside of the closure.
kontainer.use(Greeter::class) {
    println("Kontainer.use() says: ${sayHello()}")
}
```
Will output:
```
Kontainer.use() says: Hello you!
```
### Singleton are shared

This example shows that singleton services are shared across all kontainers, 
that where created from the same blueprint. 

(see the full [example](../../src/examples/_01_defining_services/SharedSingletonExample.kt))

```kotlin
// 1. We define our service
class Counter {
    private var count = 0
    fun next() = ++count
}

// 2. we create the kontainer blueprint
val blueprint = kontainer {
    singleton(Counter::class)
}

// 3. We get a kontainer instance and use the singleton
blueprint.create().let { kontainer ->
    println("Counter: ${kontainer.get(Counter::class).next()}")
}

// 4. We get a another kontainer instance and use the singleton
blueprint.create().let { kontainer ->
    println("Counter: ${kontainer.get(Counter::class).next()}")
}
```
Will output:
```
Counter: 1
Counter: 2
```
### Singletons vs Dynamics vs Prototypes

This example shows the difference between a **Singleton**, a **Dynamic** and a **Prototype** service.

Singleton are instantiated only once. They are then shared across all kontainer instances.  
Dynamic services are instantiated for each kontainer instance.

You will see that:  

The **SingletonCounter** is globally created once and is always increasing.  
The **DynamicCounter** is created once for each kontainer instance.  
The **PrototypeCounter** is created every time it is requested from the kontainer.  

(see the full [example](../../src/examples/_01_defining_services/SingletonVsDynamicVsPrototypeExample.kt))

```kotlin
// 1. We define our services
abstract class Counter {
    private var count = 0
    fun next() = ++count
}

class SingletonCounter : Counter()
class DynamicCounter : Counter()
class PrototypeCounter : Counter()

// 2. We create the kontainer blueprint
val blueprint = kontainer {
    // We register a singleton service
    singleton(SingletonCounter::class)
    // We register a dynamic service
    dynamic(DynamicCounter::class)
    // We register a prototype service
    prototype(PrototypeCounter::class)
}

// Let's create three kontainer instances
for (round in 1..3) {

    println("Round #$round")

    val kontainer = blueprint.create()

    // We are getting each service multiple times
    repeat(3) {
        val singleton = kontainer.get(SingletonCounter::class).next()
        val dynamic = kontainer.get(DynamicCounter::class).next()
        val prototype = kontainer.get(PrototypeCounter::class).next()

        println("singleton: $singleton - dynamic $dynamic - prototype: $prototype")
    }
}
```
Will output:
```
Round #1
singleton: 1 - dynamic 1 - prototype: 1
singleton: 2 - dynamic 2 - prototype: 1
singleton: 3 - dynamic 3 - prototype: 1
Round #2
singleton: 4 - dynamic 1 - prototype: 1
singleton: 5 - dynamic 2 - prototype: 1
singleton: 6 - dynamic 3 - prototype: 1
Round #3
singleton: 7 - dynamic 1 - prototype: 1
singleton: 8 - dynamic 2 - prototype: 1
singleton: 9 - dynamic 3 - prototype: 1
```
## Service Injection

### Basic Injection Example

This example shows how a service can inject another service.

For simplicity there are two ways of injection:
1. Constructor injection.
2. Factory method injection, which we will see next

(see the full [example](../../src/examples/_02_injecting_services/BasicInjectionExample.kt))

```kotlin
// We define a service that will be injected
class Counter {
    private var count = 0
    fun next() = ++count
}

// We define a service that injects another service in it's constructor
class MyService(val counter: Counter)

// We define the kontainer blueprint
val blueprint = kontainer {
    singleton(MyService::class)
    singleton(Counter::class)
}

// We get the kontainer instance
val kontainer = blueprint.create()

// We use the service and access the injected service
val myService = kontainer.get(MyService::class)

println("Next: " + myService.counter.next())
println("Next: " + myService.counter.next())
```
Will output:
```
Next: 1
Next: 2
```
### Factory Method Injection

Sometimes pure constructor injection is not enough.  

For example when you have service class that expects other parameters that are not known to the kontainer.

Factory methods are available for singletons, dynamics and prototypes from zero up to seven parameters.

**NOTICE:** There is one quirk! The factory methods with zero parameters are: 
- singleton0 { ... }
- prototype0 { ... }
- dynamic0 { ... }

(see the full [example](../../src/examples/_02_injecting_services/FactoryMethodInjectionExample.kt))

```kotlin
// We define a service that will be injected
class Counter {
    private var count = 0
    fun next() = ++count
}

// We define a service that injects another service in it's constructor.
// But this time this service also expects a second parameter that cannot be provided by the kontainer.
class MyService(private val counter: Counter, private val offset: Int) {
    fun next() = counter.next() + offset
}

// We define the kontainer blueprint
val blueprint = kontainer {
    // We define the service using a factory method.
    // Injection is now only done for all parameters of the factory method.
    singleton { counter: Counter ->
        MyService(counter, 100)
    }

    singleton(Counter::class)
}

// We get the kontainer instance
val kontainer = blueprint.create()

// We use the service
val myService = kontainer.get(MyService::class)

println("Next: " + myService.next())
println("Next: " + myService.next())
```
Will output:
```
Next: 101
Next: 102
```
### Injecting a singleton into multiple services

This example shows how one singleton service is injected into multiple services.

(see the full [example](../../src/examples/_02_injecting_services/InjectSingletonIntoMultipleServicesExample.kt))

```kotlin
// 1. We define a service that will be injected
class Counter {
    private var count = 0
    fun next() = ++count
}

// 2. We define two services that inject the counter service
class One(val counter: Counter)
class Two(val counter: Counter)

// 3. We define the kontainer blueprint
val blueprint = kontainer {
    // defining the injected service as a singleton
    singleton(Counter::class)
    // defining the consuming services
    singleton(One::class)
    singleton(Two::class)
}

// 3. We get the kontainer instance
val kontainer = blueprint.create()

// 4. We use the services and access the injected service
val one = kontainer.get(One::class)
val two = kontainer.get(Two::class)

println("One: " + one.counter.next())
println("Two: " + two.counter.next())
```
Will output:
```
One: 1
Two: 2
```
### Injecting a prototype into multiple services

This example shows how a prototype service is injected into multiple services.

(see the full [example](../../src/examples/_02_injecting_services/InjectPrototypeIntoMultipleServicesExample.kt))

```kotlin
// 1. We define a service that will be injected
class Counter {
    private var count = 0
    fun next() = ++count
}

// 2. We define two services that inject the counter service
class One(val counter: Counter)
class Two(val counter: Counter)

// 3. We define the kontainer blueprint
val blueprint = kontainer {
    // defining the injected service as a prototype
    prototype(Counter::class)
    // defining the consuming services
    singleton(One::class)
    singleton(Two::class)
}

// 3. We get the kontainer instance
val kontainer = blueprint.create()

// 4. We use the services and access the injected service
val one = kontainer.get(One::class)
val two = kontainer.get(Two::class)

println("One: " + one.counter.next())
println("Two: " + two.counter.next())
```
Will output:
```
One: 1
Two: 1
```
### Injection By SuperType

This example shows how a service can be injected by one of its supertypes.

(see the full [example](../../src/examples/_02_injecting_services/InjectBySuperTypeExample.kt))

```kotlin
// We define a service that extends or implements a super type
interface CounterInterface {
    fun next(): Int
}

class Counter : CounterInterface {
    private var count = 0
    override fun next() = ++count
}

// We inject a service by it's interface
class MyService(val counter: CounterInterface)

// We define the kontainer blueprint
val blueprint = kontainer {
    singleton(Counter::class)
    singleton(MyService::class)
}

// We get the kontainer instance
val kontainer = blueprint.create()

// We use the service and access the injected service
val myService = kontainer.get(MyService::class)

println("Next: " + myService.counter.next())
```
Will output:
```
Next: 1
```
### Injection of all Services By SuperType

This example shows how to inject all services that implement or extend a given super type.

This is very useful when we design systems for extensibility.

For Example:  
Let's say we have a Database service that injects all registered Repositories.
Repositories can then even by added by code that is not maintained by us. 

(see the full [example](../../src/examples/_02_injecting_services/InjectAllBySuperTypeExample.kt))

```kotlin
// We define an interface for all repositories
interface Repository {
    val name: String
}

// We create some implementations
class UserRepository : Repository {
    override val name = "users"
}

class OrderRepository : Repository {
    override val name = "orders"
}

// We inject all Repositories into our service
class MyService(val repos: List<Repository>)

// We define the kontainer blueprint
val blueprint = kontainer {
    singleton(MyService::class)

    singleton(UserRepository::class)
    singleton(OrderRepository::class)
}

// We get the kontainer instance
val kontainer = blueprint.create()

// We use the service
val myService = kontainer.get(MyService::class)

// We get all injected Repos
myService.repos.forEach {
    println(it.name)
}
```
Will output:
```
users
orders
```
### Lazy Injection Example

This example shows how a to lazily inject a service.

This means that the injected service will only be instantiated when it used for the first time. 

(see the full [example](../../src/examples/_02_injecting_services/LazyInjectionExample.kt))

```kotlin
// We define a service that will be injected
class LazilyInjected {
    fun sayHello() = "Here I am!"
}

// We define a service that lazily injects another service in it's constructor
class MyService(private val lazy: Lazy<LazilyInjected>) {
    fun sayHello() = lazy.value.sayHello()
}

// We define the kontainer blueprint
val blueprint = kontainer {
    singleton(MyService::class)
    singleton(LazilyInjected::class)
}

// We get the kontainer instance
val kontainer = blueprint.create()

// We use the service and access the injected service
val myService = kontainer.get(MyService::class)

println("We got MyService from the kontainer")
println("# instances of the LazilyInjected:   ${kontainer.getProvider(LazilyInjected::class).instances.size}")

println("We used the lazily injected service: ${myService.sayHello()}")
println("# instances of the LazilyInjected:   ${kontainer.getProvider(LazilyInjected::class).instances.size}")
```
Will output:
```
We got MyService from the kontainer
# instances of the LazilyInjected:   0
We used the lazily injected service: Here I am!
# instances of the LazilyInjected:   1
```
### Breaking Cyclic Dependencies with Lazy Injection

In some cases we have services that need to injected each other.
 
This cyclic dependency can be broken with lazy injection. 

(see the full [example](../../src/examples/_02_injecting_services/LazyInjectionCycleBreakerExample.kt))

```kotlin
// We define two services that inject each other
class ServiceOne(private val two: ServiceTwo) {
    val name = "one"
    fun sayHello() = "I am ServiceOne and I know '${two.name}'"
}

class ServiceTwo(private val one: Lazy<ServiceOne>) {
    val name = "two"
    fun sayHello() = "I am ServiceTwo and I know '${one.value.name}'"
}

// We define the kontainer blueprint
val blueprint = kontainer {
    singleton(ServiceOne::class)
    singleton(ServiceTwo::class)
}

// We get the kontainer instance
val kontainer = blueprint.create()

// We use both services
val one = kontainer.get(ServiceOne::class)
val two = kontainer.get(ServiceTwo::class)

println(one.sayHello())
println(two.sayHello())
```
Will output:
```
I am ServiceOne and I know 'two'
I am ServiceTwo and I know 'one'
```
### Lazily inject all Services By SuperType

This example shows how to lazily inject all services of a given super type.

(see the full [example](../../src/examples/_02_injecting_services/LazyInjectAllBySuperTypeExample.kt))

```kotlin
// We define an interface for all repositories
interface Repository {
    val name: String
}

// We create some implementations
class UserRepository : Repository {
    override val name = "users"
}

class OrderRepository : Repository {
    override val name = "orders"
}

// We inject all Repositories into our service
class MyService(val repos: Lazy<List<Repository>>)

// We define the kontainer blueprint
val blueprint = kontainer {
    singleton(MyService::class)

    dynamic(UserRepository::class)
    dynamic(OrderRepository::class)
}

// We get the kontainer instance
val kontainer = blueprint.create()

// We use the service
val myService = kontainer.get(MyService::class)

// We get all injected Repos
myService.repos.value.forEach {
    println(it.name)
}
```
Will output:
```
users
orders
```
### Lazily inject all Services By SuperType with a Lookup

This example shows how to lazily inject all services of a given super type using a lookup.

What is this good for? 

By using a lazy lookup we can inject many services without instantiating them.  
We can get individual services from the LookUp by their class.

(see the full [example](../../src/examples/_02_injecting_services/LazyInjectAllBySuperTypeWithLookUpExample.kt))

```kotlin
// We define an interface for all repositories
interface Repository {
    val name: String
}

// We create some implementations
class UserRepository : Repository {
    override val name = "users"
}

class OrderRepository : Repository {
    override val name = "orders"
}

// We inject all Repositories into our service as a Lookup
class MyService(val repos: Lookup<Repository>)

// We define the kontainer blueprint
val blueprint = kontainer {
    singleton(MyService::class)

    dynamic(UserRepository::class)
    dynamic(OrderRepository::class)
}

// We get the kontainer instance
val kontainer = blueprint.create()

// We use the service
val myService = kontainer.get(MyService::class)

// UserRepository is not yet instantiated
println("# instances of UserRepository ${kontainer.getProvider(UserRepository::class).instances.size}")
// We get is from the Lookup
println("Getting it from the Lookup: " + myService.repos.get(UserRepository::class).name)
// It is now instantiated
println("# instances of UserRepository ${kontainer.getProvider(UserRepository::class).instances.size}")

// OrderRepository is not yet instantiated
println("# instances of OrderRepository ${kontainer.getProvider(OrderRepository::class).instances.size}")
// We get is from the Lookup
println("Getting it from the Lookup: " + myService.repos.get(OrderRepository::class).name)
// It is now instantiated
println("# instances of OrderRepository ${kontainer.getProvider(OrderRepository::class).instances.size}")
```
Will output:
```
# instances of UserRepository 0
Getting it from the Lookup: users
# instances of UserRepository 1
# instances of OrderRepository 0
Getting it from the Lookup: orders
# instances of OrderRepository 1
```