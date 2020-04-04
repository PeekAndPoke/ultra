# Examples for ultra::kontainer

## TOC
0. The Basics

    0. [Simple Singleton Example](#Simple Singleton Example)
    1. [Re-using a singleton service across multiple kontainer instances](#Re-using a singleton service across multiple kontainer instances)
    2. [Singletons vs Dynamic Services](#Singletons vs Dynamic Services)
## The Basics

### Simple Singleton Example
```kotlin
// 1. we create the kontainer blueprint
val blueprint = kontainer {
    singleton(Greeter::class)
}

// 2. we get the kontainer instance
val kontainer = blueprint.create()

// 3. we retrieve by kontainer.use(...)
kontainer.use(Greeter::class) {
    println("Kontainer.use() says: ${sayHello()}")
}

// 4. we retrieve by kontainer.get(...)
println(
    "Kontainer.get() says: ${kontainer.get(Greeter::class).sayHello()}"
)
```
Will output:
```
Kontainer.use() says: Hello you!
Kontainer.get() says: Hello you!
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

### Singletons vs Dynamic Services
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

