# ULTRA

ULTRA is a set of libraries and helpers to enhance your everyday Kotlin coding with more power, stability and enjoyment.

Check out the sub-projects. There you will find details, descriptive test cases and usage examples.

## ultra::common

[ultra::common](ultra/common/README.MD) - Multiplatform Helpers and utilities for:

- Collections, Lists, Files, Functions
- Hashing (like MD5, SHA256, ...)
- Utility Classes and Interfaces
- Recursive programming
    - map recursive objects to plain lists
        - find all child objects that match a certain criteria
- Reflection
    - type safe type references
    - reification of KType
- random data generation
    - LoremIpsum texts

## ultra::kontainer

[ultra::kontainer](ultra/kontainer/README.MD) - A multi stage dependency injection mechanism

[READ MORE](ultra/kontainer/README.MD) ||
[EXAMPLES](ultra/kontainer/docs/ultra::docs/index.md) ||
[TESTS](ultra/kontainer/src/test/kotlin)

## ultra::logging

[ultra::logging](ultra/logging/README.MD) - A lean logging framework that integrates with the Kontainer

## ultra::meta

[ultra::meta](ultra/meta/README.MD) - Meta Programming Tools for Kotlin Annotation Processing and code generation

Projects using this:

- ultra::slumber
- ultra::vault
- karango (ArangoDB repositories for ultra::vault)

## ultra::mutator

[ultra::mutator](ultra/mutator/README.MD) - Mutate the Immutable!

[READ MORE](ultra/mutator/README.MD) ||
[EXAMPLES](ultra/mutator/docs/ultra::docs/index.md) ||
[TESTS](ultra/mutator/src/test/kotlin)

Mutator is a kotlin library inspired by [immerjs](https://github.com/immerjs/immer).  
It enables you to manipulate deeply nested immutable data structures.  
With zero boilerplate code at all and some powerful additional features.

- It gives you a powerful way for mutating deeply nested immutable data structures.
- By doing so, it encourages a 100% immutable data design
- ... and more of functional programming style.
- Therefore you will have far less tricky bugs.

## ultra::security

[ultra::security](ultra/security/README.MD) - Everyday security features

## ultra::slumber

[ultra::slumber](ultra/slumber/README.MD) - Object serialization with nullability awareness

[READ MORE](ultra/slumber/README.MD) ||
[EXAMPLES](ultra/mutator/docs/ultra::docs/index.md) ||
[TESTS](ultra/mutator/src/test/kotlin)

Slumber is aware of the Null Safety that is built into Kotlin.

Pure Java serialization libraries like [Jackson](https://github.com/FasterXML/jackson) are not.

Kotlin handles the [Billion Dollar Mistake](https://en.wikipedia.org/wiki/Tony_Hoare#Apologies_and_retractions)
very well. And so should your serialization framework! Slumber does it for you.

# TODOs:

- ( ) Remove com.github.matfax.klassindex:library completely
    - ( ) Replace with own implementation using KSP

- ( ) Slumber
    - ( ) Full java time support

- ( ) Mutator
    - ( ) support better code generation for nested classes
  
