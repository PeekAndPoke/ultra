# Examples for ultra::kontainer

## TOC

1. [The Basics](#the-basics)

    1. [Simple Singleton Example](#simple-singleton-example)
    2. [Shared Singleton Services](#shared-singleton-services)
    3. [Singletons vs Dynamic vs Prototype](#singletons-vs-dynamic-vs-prototype)
2. [Service Injection](#service-injection)

    1. [Basic Injection Example](#basic-injection-example)
    2. [Factory Method Injection](#factory-method-injection)
    3. [Injecting a singleton into multiple services](#injecting-a-singleton-into-multiple-services)
    4. [Injecting a prototype into multiple services](#injecting-a-prototype-into-multiple-services)

## The Basics

### Simple Singleton Example

This example shows how to register and retrieve a simple singleton service.

Services can be retrieved by:
1. kontainer.get(...)
2. kontainer.use(...)

@see the [runnable example](../../src/examples/_01_the_basics/BasicSingletonExample.kt)

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
### Shared Singleton Services

This example shows that singleton services are shared across all kontainers, 
that where created from the same blueprint. 

@see the [runnable example](../../src/examples/_01_the_basics/SharedSingletonExample.kt)

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
### Singletons vs Dynamic vs Prototype

This example shows the difference between a **singleton**, a **dynamic** and a **prototype** service.

Singleton are instantiated only once. They are then shared across all kontainer instances.  
Dynamic services are instantiated for each kontainer instance.

You will see that:  

The **SingletonCounter** is globally created once and is always increasing.  
The **DynamicCounter** is created once for each kontainer instance.  
The **PrototypeCounter** is created every time it is requested from the kontainer.  

@see the [runnable example](../../src/examples/_01_the_basics/SingletonVsDynamicVsPrototypeExample.kt)

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

@see the [runnable example](../../src/examples/_02_injection/BasicInjectionExample.kt)

```kotlin
// 1. We define a service that will be injected
class Counter {
    private var count = 0
    fun next() = ++count
}

// 2. We define a service that injects another service in it's constructor
class MyService(val counter: Counter)

// 3. We define the kontainer blueprint
val blueprint = kontainer {
    singleton(MyService::class)
    singleton(Counter::class)
}

// 3. We get the kontainer instance
val kontainer = blueprint.create()

// 4. We use the service and access the injected service
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

@see the [runnable example](../../src/examples/_02_injection/FactoryMethodInjectionExample.kt)

```kotlin
// 1. We define a service that will be injected
class Counter {
    private var count = 0
    fun next() = ++count
}

// 2. We define a service that injects another service in it's constructor.
//    But this time this service also expects a second parameter that cannot be provided by the kontainer.
class MyService(private val counter: Counter, private val offset: Int) {
    fun next() = counter.next() + offset
}

// 3. We define the kontainer blueprint
val blueprint = kontainer {
    // We define the service using a factory method.
    // Injection is now only done for all parameters of the factory method.
    singleton { counter: Counter ->
        MyService(counter, 100)
    }

    singleton(Counter::class)
}

// 3. We get the kontainer instance
val kontainer = blueprint.create()

// 4. We use the service and access the injected service
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

@see the [runnable example](../../src/examples/_02_injection/InjectSingletonIntoMultipleServicesExample.kt)

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

@see the [runnable example](../../src/examples/_02_injection/InjectPrototypeIntoMultipleServicesExample.kt)

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
