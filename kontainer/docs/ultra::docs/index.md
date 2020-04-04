# Examples for ultra::kontainer

## TOC

1. [The Basics](#the-basics)
    1. [Simple Singleton Example](#simple-singleton-example)
    2. [Shared Singleton Services](#shared-singleton-services)
    3. [Singletons vs Dynamic Services](#singletons-vs-dynamic-services)

## The Basics

### Simple Singleton Example

This example show how to register and retrieve a simple singleton service.

Services can be retrieved by:
1. kontainer.get(...)
2. kontainer.use(...)

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

This example demonstrates that singleton services are shared across all kontainers, that where
created from the same blueprint. 

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
    println(
        "Counter: ${kontainer.get(Counter::class).next()}"
    )
}

// 4. We get a another kontainer instance and use the singleton
blueprint.create().let { kontainer ->
    println(
        "Counter: ${kontainer.get(Counter::class).next()}"
    )
}
```
Will output:
```
Counter: 1
Counter: 2
```

### Singletons vs Dynamic Services

This example demonstrates the difference between a singleton and a dynamic service.

Singleton are instantiated only once. They are then shared across all kontainer instances.  
Dynamic services are instantiated for each kontainer instance.

You will see that the **SingletonCounter** is always increasing.  
The **DynamicCounter** is created for each kontainer instance.

```kotlin
// 1. We define our services
class SingletonCounter {
    private var count = 0
    fun next() = ++count
}

class DynamicCounter {
    private var count = 0
    fun next() = ++count
}

// 2. we create the kontainer blueprint
val blueprint = kontainer {
    singleton(SingletonCounter::class)
    dynamic(DynamicCounter::class)
}

// 3. We get a kontainer instance and use the singleton
blueprint.create().let { kontainer ->

    println("First kontainer instance:")

    // We are getting each service multiple times
    repeat(3) {
        val singleton = kontainer.get(SingletonCounter::class).next()
        val dynamic = kontainer.get(DynamicCounter::class).next()

        println("singleton: $singleton - dynamic $dynamic")
    }
}

// 4. We get a another kontainer instance and use the singleton
blueprint.create().let { kontainer ->

    println("Second kontainer instance:")

    // We are getting each service multiple times
    repeat(3) {
        val singleton = kontainer.get(SingletonCounter::class).next()
        val dynamic = kontainer.get(DynamicCounter::class).next()

        println("singleton: $singleton - dynamic $dynamic")
    }
}
```
Will output:
```
First kontainer instance:
singleton: 1 - dynamic 1
singleton: 2 - dynamic 2
singleton: 3 - dynamic 3
Second kontainer instance:
singleton: 4 - dynamic 1
singleton: 5 - dynamic 2
singleton: 6 - dynamic 3
```

