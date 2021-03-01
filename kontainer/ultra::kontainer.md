# ultra::kontainer

Kontainer is a multi stage dependency injection mechanism.

It ensures that the whole system is consistent at any time.

It supports dynamic services that are especially useful in the context of a server application. Dynamic services can
carry state and can therefore collect data about a single request. Dynamic services only life for the duration of a
single request.

## Examples

Code says more than a thousand words. So let's look at the [Examples](docs/ultra::docs/index.md)!

For more inspiration you can also have a look at the [Test](src/test/kotlin)!

## What does "multi stage" mean?

To answer this, let's first have a look at how (to our best knowledge) dependency injection is usually implemented in
the java world (e.g. in Spring) and compare it with the Kontainer.

| The conventional way                                             | The Kontainer way                                                                      |
| ---------------------------------------------------------------- | -------------------------------------------------------------------------------------- |
| 1. You define your services (via annotations or xml)             | 1. You define your services in pure Kotlin code                                        |
| 2. Service types are Singleton and Prototype                     | 2. Services types are Singleton, Prototype and **
Dynamic**                             |
| 3. You define which services are to be injected                  | 3. Injection is done through the primary constructor                                   |    
| 4. The container does all the auto-wiring.                       | 4. Phase 1: You create a Kontainer Blueprint                                           |
| 5. Services can be requested from the container                  | 5. Phase 2: You obtain a Kontainer from the Blueprint                                  |
|                                                                  | 6. Services can be requested from the Kontainer instance                               |
|                                                                  |                                                                                        |   
| **Pros**                                                         | **Pros**                                                                               |
|                                                                  |                                                                                        |
| service needs to be instantiated exactly once                    | **Dynamic** Services are instantiated for each Kontainer instance                      |
| every service instance is fully re-usable                        | So do services that injecte **Dynamic** services directly or indirectly                |
|   -> e.g. for each request                                       | Kontainers and **Dynamic** services can be aware of the **Context** used in            |
|                                                                  |   -> e.g. the current request                                                          | 
|                                                                  |   -> e.g. the current user session                                                     | 
|                                                                  | **Dynamic** services may hold state                                                    | 
|                                                                  |   -> as they are cleaned up when the Kontainer instance is shut down                   | 
|                                                                  |   -> e.g. gathering logs, database request, ... per request                            | 
|                                                                  | Kontainer Blueprints ensure that Kontainers are consistent                             | 
|                                                                  |                                                                                        |
| **Cons**                                                         | **Cons**                                                                               |
|                                                              |                                                                                        |
| everything is static                                             | **Dynamic** and **Semi-Dynamic** services are instantiated for each Kontainer instance |
| services are not aware of the **Context** they are used in, e.g. |                                                                                        |   
|   -> the current request                                         |                                                                                        |   
|   -> the current user session                                    |                                                                                        |   
| context awareness can be achieved                                |                                                                                        |   
|   -> by passing around information -> leads to tight coupling    |                                                                                        |   
|   -> by Thread-Level hacks -> which is complex and brittle       |                                                                                        |   
| services may never hold any state                                |                                                                                        |   
|   -> due to the absence of context                               |                                                                                        |   
|   -> the state is never cleaned up                               |                                                                                        |   
|   -> it might cause problems with parallel programming           |                                                                                        |   

