# Examples

## The Basics

### Registering and retrieving a singleton service with kontainer.use(...)
```kotlin
// 1. we create the kontainer blueprint
val blueprint = kontainer {
    singleton(Greeter::class)
}

// 2. we get the kontainer instance
val kontainer = blueprint.create()

// 3. we retrieve and use a service
kontainer.use(Greeter::class) {
    println("The service says: ${sayHello()}")
}
```
Will output:
```
The service says: Hello you!
```

### Registering and retrieving a singleton service with kontainer.get(...)
```kotlin
// 1. we create the kontainer blueprint
val blueprint = kontainer {
    singleton(Greeter::class)
}

// 2. we get the kontainer instance
val kontainer = blueprint.create()

// 3. we retrieve and use a service
println(
    "The service says: ${kontainer.get(Greeter::class).sayHello()}"
)
```
Will output:
```
The service says: Hello you!
```

### Re-using a singleton service across multiple kontainer instances 
```kotlin
// 1. we create the kontainer blueprint
val blueprint = kontainer {
    singleton(Counter::class)
}

// 2. We get a kontainer instance and use the singleton
blueprint.create().let { kontainer ->
    println(
        "Counter: ${kontainer.get(Counter::class).next()}"
    )
}

// 3. We get a another kontainer instance and use the singleton
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

### This example demonstrates the difference between a singleton and a dynamic service.

Singleton are instantiated only once. They are then shared across all kontainer instances.  
Dynamic services are instantiated for each kontainer instance.

You will see that the **SingletonCounter** is always increasing.  
The **DynamicCounter** is created for each kontainer instance.
```kotlin
// 1. we create the kontainer blueprint
val blueprint = kontainer {
    singleton(SingletonCounter::class)
    dynamic(DynamicCounter::class)
}

// 2. We get a kontainer instance and use the singleton
blueprint.create().let { kontainer ->

    println("First kontainer instance:")
    println()

    // We are getting each service multiple times
    repeat(3) {
        println(
            "SingletonCounter: ${kontainer.get(SingletonCounter::class).next()} -" +
                    "DynamicCounter: ${kontainer.get(DynamicCounter::class).next()}"
        )
    }
}

// 3. We get a another kontainer instance and use the singleton
blueprint.create().let { kontainer ->

    println("Second kontainer instance:")
    println()

    // We are getting each service multiple times
    repeat(3) {
        println(
            "SingletonCounter: ${kontainer.get(SingletonCounter::class).next()} - " +
                    "DynamicCounter: ${kontainer.get(DynamicCounter::class).next()}"
        )
    }
}
```
Will output:
```
First kontainer instance:
SingletonCounter: 1 -DynamicCounter: 1
SingletonCounter: 2 -DynamicCounter: 2
SingletonCounter: 3 -DynamicCounter: 3
Second kontainer instance:
SingletonCounter: 4 - DynamicCounter: 1
SingletonCounter: 5 - DynamicCounter: 2
SingletonCounter: 6 - DynamicCounter: 3
```

