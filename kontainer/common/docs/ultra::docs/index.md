# Examples for ultra::kontainer

## TOC

1. [Defining Services](#DEFINING-SERVICES)

    1. [Defining a Singleton service](#DEFINING-A-SINGLETON-SERVICE)
    2. [Defining a Dynamic service](#DEFINING-A-DYNAMIC-SERVICE)
    3. [Defining a Prototype service](#DEFINING-A-PROTOTYPE-SERVICE)
    4. [Defining an existing object as a Singleton service](#DEFINING-AN-EXISTING-OBJECT-AS-A-SINGLETON-SERVICE)
    5. [Hiding the concrete implementation of a service](#HIDING-THE-CONCRETE-IMPLEMENTATION-OF-A-SERVICE)
    6. [Singletons are shared](#SINGLETONS-ARE-SHARED)
    7. [Singletons vs Dynamics vs Prototypes](#SINGLETONS-VS-DYNAMICS-VS-PROTOTYPES)

2. [Service Injection](#SERVICE-INJECTION)

    1. [Basic Injection Example](#BASIC-INJECTION-EXAMPLE)
    2. [Factory Method Injection](#FACTORY-METHOD-INJECTION)
    3. [Injecting a singleton into multiple services](#INJECTING-A-SINGLETON-INTO-MULTIPLE-SERVICES)
    4. [Injecting a prototype into multiple services](#INJECTING-A-PROTOTYPE-INTO-MULTIPLE-SERVICES)
    5. [Nullable Injection](#NULLABLE-INJECTION)
    6. [Injection By SuperType](#INJECTION-BY-SUPERTYPE)
    7. [Injection By SuperType can fail due to ambiguity](#INJECTION-BY-SUPERTYPE-CAN-FAIL-DUE-TO-AMBIGUITY)
    8. [Injection of all Services By SuperType](#INJECTION-OF-ALL-SERVICES-BY-SUPERTYPE)
    9. [Lazy Injection Example](#LAZY-INJECTION-EXAMPLE)
    10. [Breaking Cyclic Dependencies with Lazy Injection](#BREAKING-CYCLIC-DEPENDENCIES-WITH-LAZY-INJECTION)
    11. [Lazily inject all Services By SuperType](#LAZILY-INJECT-ALL-SERVICES-BY-SUPERTYPE)
    12. [Lazily inject all Services By SuperType with a Lookup](#LAZILY-INJECT-ALL-SERVICES-BY-SUPERTYPE-WITH-A-LOOKUP)

3. [Defining Modules](#DEFINING-MODULES)

    1. [Defining a simple module](#DEFINING-A-SIMPLE-MODULE)
    2. [Parameterized modules](#PARAMETERIZED-MODULES)

## Defining Services

### Defining a Singleton service

This example shows how to register and retrieve a simple singleton service.

Singleton services are created only once. They are shared across all kontainer instances that
are created from the same kontainer blueprint.

Services can be retrieved by:

1. kontainer.get(...)
2. kontainer.use(...)

(see the full [example](../../../src/examples/defining_services/DefiningASingletonExample.kt))

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

(see the full [example](../../../src/examples/defining_services/DefiningADynamicExample.kt))

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

(see the full [example](../../../src/examples/defining_services/DefiningAPrototypeExample.kt))

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

(see the full [example](../../../src/examples/defining_services/DefiningAnExistingInstanceExample.kt))

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

### Hiding the concrete implementation of a service

This example shows how to hide the concrete implementation of a service.

The same mechanism is available for all types of service registration:

- singleton
- dynamic
- prototype
- instance
- factory methods

(see the full [example](../../../src/examples/defining_services/HidingTheConcreteImplementationOfAServiceExample.kt))

```kotlin
// Let's say we have an interface that defines one of our services.
interface GreeterInterface {
    fun sayHello(): String
}

// And we have an implementation.
class Greeter : GreeterInterface {
    override fun sayHello() = "Hello you!"
}

val blueprint = kontainer {
    // Then we can define the service the following way, which means:
    // The kontainer will only know that there is a service of type GreeterInterface.
    singleton(GreeterInterface::class, Greeter::class)
}

val kontainer = blueprint.create()

// We can now retrieve the GreeterInterface service and use it.
println(
    "It says: ${kontainer.get(GreeterInterface::class).sayHello()}"
)

// But when we try to retrieve the concrete implementation we will get an error.
try {
    kontainer.get(Greeter::class)
} catch (e: ServiceNotFound) {
    println(e)
}
```

Will output:

```
It says: Hello you!
de.peekandpoke.ultra.kontainer.ServiceNotFound: Service de.peekandpoke.ultra.kontainer.examples.defining_services.HidingTheConcreteImplementationOfAServiceExample.Greeter was not found
```

### Singletons are shared

This example shows that singleton services are shared across all kontainers,
that are created from the same blueprint.

(see the full [example](../../../src/examples/defining_services/SharedSingletonExample.kt))

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

// 4. We get another kontainer instance and use the singleton
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

**Singleton** services are instantiated only once. They are then shared across all kontainer instances.  
**Dynamic** services are instantiated for each kontainer instance.  
**Prototype** services are instantiated each time they are requested from the kontainer.

You will see that:

The **SingletonCounter** is globally created once and is always increasing.  
The **DynamicCounter** is created once for each kontainer instance.  
The **PrototypeCounter** is created every time it is requested from the kontainer.

(see the full [example](../../../src/examples/defining_services/SingletonVsDynamicVsPrototypeExample.kt))

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

For simplicity there are only two ways of injection:

1. Constructor injection.
2. Factory method injection, which we will see in later examples.

(see the full [example](../../../src/examples/injecting_services/BasicInjectionExample.kt))

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

For example when you have a service class expecting constructor parameters
that are not known to the kontainer.

Factory methods are available for singletons, dynamics and prototypes from zero up to ten parameters.

(see the full [example](../../../src/examples/injecting_services/FactoryMethodInjectionExample.kt))

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
    singleton(MyService::class) { counter: Counter ->
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

(see the full [example](../../../src/examples/injecting_services/InjectSingletonIntoMultipleServicesExample.kt))

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

(see the full [example](../../../src/examples/injecting_services/InjectPrototypeIntoMultipleServicesExample.kt))

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

### Nullable Injection

This example shows how to inject a service only when it is present in the kontainer.

This can be done by marking the injected parameter as nullable.

(see the full [example](../../../src/examples/injecting_services/NullableInjectionExample.kt))

```kotlin
// We define a service that will NOT be registered in the kontainer
class NotRegisteredInKontainer

// We define a service that injects the other service as nullable
class FirstService(val injected: NotRegisteredInKontainer?)

// We define another one to demonstrate the same behaviour with a factory method
class SecondService(val injected: NotRegisteredInKontainer?)

// We define the kontainer blueprint
val blueprint = kontainer {
    // We define the first service as a singleton
    singleton(FirstService::class)
    // We define the other service with a factory method (notice the nullable closure parameter)
    singleton(SecondService::class) { injected: NotRegisteredInKontainer? ->
        SecondService(injected)
    }
}

// We get the kontainer instance
val kontainer = blueprint.create()

// We use the service and access the injected service
val firstService = kontainer.get(FirstService::class)
println("FirstService.injected: " + firstService.injected)

val secondService = kontainer.get(SecondService::class)
println("SecondService.injected: " + secondService.injected)
```

Will output:

```
FirstService.injected: null
SecondService.injected: null
```

### Injection By SuperType

This example shows how a service can be injected by one of its supertypes.

(see the full [example](../../../src/examples/injecting_services/InjectBySuperTypeExample.kt))

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

### Injection By SuperType can fail due to ambiguity

This example shows that injecting by supertype can fail, when there is an ambiguity.

(see the full [example](../../../src/examples/injecting_services/InjectBySuperTypeFailsToAmbiguityExample.kt))

```kotlin
// We define a supertype
interface CounterInterface {
    fun next(): Int
}

// We create two implementations of the interface
class CounterOne : CounterInterface {
    private var count = 0
    override fun next() = ++count
}

class CounterTwo : CounterInterface {
    private var count = 0
    override fun next() = ++count
}

// We try to inject the supertype CounterInterface
class MyService(val counter: CounterInterface)

val blueprint = kontainer {
    // We register our service
    singleton(MyService::class)
    // We register both implementations of the CounterInterface
    singleton(CounterOne::class)
    singleton(CounterTwo::class)
}

// When we try to create a kontainer instance we will get an error
try {
    blueprint.create()
} catch (e: KontainerInconsistent) {
    println(e)
}
```

Will output:

```
de.peekandpoke.ultra.kontainer.KontainerInconsistent: Kontainer is inconsistent!

Problems:

1. Service 'de.peekandpoke.ultra.kontainer.examples.injecting_services.InjectBySuperTypeFailsToAmbiguityExample.MyService'
    defined at de.peekandpoke.ultra.kontainer.examples.injecting_services.InjectBySuperTypeFailsToAmbiguityExample$run$blueprint$1.invoke(InjectBySuperTypeFailsToAmbiguityExample.kt:44))
    -> Parameter 'counter' is ambiguous. The following services collide: de.peekandpoke.ultra.kontainer.examples.injecting_services.InjectBySuperTypeFailsToAmbiguityExample.CounterOne, de.peekandpoke.ultra.kontainer.examples.injecting_services.InjectBySuperTypeFailsToAmbiguityExample.CounterTwo

Config values:
```

### Injection of all Services By SuperType

This example shows how to inject all services that implement or extend a given super type.

This is very useful when we design systems for extensibility.

For Example:  
Let's say we have a Database service that injects all registered Repositories.
Repositories can then even by added by code that is not maintained by us.

(see the full [example](../../../src/examples/injecting_services/InjectAllBySuperTypeExample.kt))

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

This means that the injected service will only be instantiated when it is used for the first time.

(see the full [example](../../../src/examples/injecting_services/LazyInjectionExample.kt))

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

(see the full [example](../../../src/examples/injecting_services/LazyInjectionCycleBreakerExample.kt))

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

(see the full [example](../../../src/examples/injecting_services/LazyInjectAllBySuperTypeExample.kt))

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

(see the full [example](../../../src/examples/injecting_services/LazyInjectAllBySuperTypeWithLookUpExample.kt))

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

## Defining Modules

Kontainer Modules are a very useful and simple way to group services together.

Library developers can use them to bundle up their library and to provide easy ways to:

- integrate the library into the kontainer of an application
- document the library and customization options on a high level

A user of a kontainer module can then simply include the module into the kontainer definition.

A module can also give a nice high level documentation of what the library does and how to customize it.

For example there could be some comments in the module definition code, that explain which services can be
overridden to achieve different behaviours of the library.

### Defining a simple module

This example shows how to define a kontainer module.

(see the full [example](../../../src/examples/defining_modules/SimpleModuleExample.kt))

```kotlin
// Let's say we have some services

// A database service
class Database(val storage: Storage)

// A Storage interface
interface Storage {
    val name: String
}

// And by default we only deliver our module with a FileStorage implementation
class FileStorage : Storage {
    override val name = "FileStorage"
}

// We can now define a kontainer module like this
val ourModule = module {
    singleton(Database::class)

    // The storage service can be overridden by the user of the module
    singleton(Storage::class, FileStorage::class)
}

// Now we can use the module when defining a kontainer blueprint
val blueprint = kontainer {
    module(ourModule)
}

val kontainer = blueprint.create()

println("Storage service: " + kontainer.get(Database::class).storage.name)

// /////////////////////////////////////////////////////////////////////////////////////////////////////////////

// Let's create another implementation of Storage and override the default service
class MemoryStorage : Storage {
    override val name = "MemoryStorage"
}

val blueprintEx = kontainer {
    module(ourModule)

    // Here we override the pre-defined Storage implementation
    singleton(Storage::class, MemoryStorage::class)
}

val kontainerEx = blueprintEx.create()

println("Storage service now is: " + kontainerEx.get(Database::class).storage.name)
```

Will output:

```
Storage service: FileStorage
Storage service now is: MemoryStorage
```

### Parameterized modules

This example shows how to define a parameterized kontainer module.

A module can take up to three parameters.

(see the full [example](../../../src/examples/defining_modules/ParameterizedModuleExample.kt))

```kotlin
class Service(val sum: Int)

// We can now define a kontainer module with up to three parameters like this
// The parameters can be of any type. For simplicity of the example we only use Ints here.
val ourModule = module { a: Int, b: Int, c: Int ->
    instance(Service(a + b + c))
}

// Now we can use the module when defining a kontainer blueprint
val blueprint = kontainer {
    module(ourModule, 1, 10, 100)
}

val kontainer = blueprint.create()

println("Sum: " + kontainer.get(Service::class).sum)
```

Will output:

```
Sum: 111
```

