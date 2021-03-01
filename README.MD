# ULTRA components for Kotlin Servers and Android

[![CodeFactor](https://www.codefactor.io/repository/github/peekandpoke/ultra/badge)](https://www.codefactor.io/repository/github/peekandpoke/ultra)

ULTRA is a set of libraries and helpers to enhance your everyday Kotlin coding with more power, stability and enjoyment.

Check out the sub-projects. There you will find details, descriptive test cases and usage examples.

## TODO: publish on Maven Central

How to:

Description see here:
https://getstream.io/blog/publishing-libraries-to-mavencentral-2021/

Sonatype JIRA:
https://issues.sonatype.org/browse/OSSRH-65096

## ultra::common

[ultra::common](common/ultra::common.md) - Helper functions and utilities for:

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

[ultra::kontainer](kontainer/ultra::kontainer.md) - A multi stage dependency injection mechanism  

[READ MORE](kontainer/ultra::kontainer.md) ||
[EXAMPLES](kontainer/docs/ultra::docs/index.md) ||
[TESTS](kontainer/src/test/kotlin)

## ultra::logging

[ultra::logging](logging/ultra::logging.md) - A lean logging framework that integrates with the Kontainer

## ultra::meta

[ultra::meta](meta/ultra::meta.md) - Meta Programming Tools for Kotlin Annotation Processing and code generation

Projects using this:

- ultra::slumber
- ultra::vault
- karango (ArangoDB repositories for ultra::vault)

## ultra::mutator

[ultra::mutator](mutator/ultra::mutator.md) - Mutate the Immutable!

[READ MORE](mutator/ultra::mutator.md) ||
[EXAMPLES](mutator/docs/ultra::docs/index.md) ||
[TESTS](mutator/src/test/kotlin)

Mutator is a kotlin library inspired by [immerjs](https://github.com/immerjs/immer).  
It enables you to manipulate deeply nested immutable data structures.  
With zero boilerplate code at all and some powerful additional features.

- It gives you a powerful way for mutating deeply nested immutable data structures.
- By doing so, it encourages a 100% immutable data design
- ... and more of functional programming style.
- Therefore you will have far less tricky bugs.

## ultra::security

[ultra::security](security/ultra::security.md) - Everyday security features

## ultra::slumber

[ultra::slumber](slumber/ultra::slumber.md) - Object serialization with nullability awareness

[READ MORE](slumber/ultra::slumber.md) ||
[EXAMPLES](mutator/docs/ultra::docs/index.md) ||
[TESTS](mutator/src/test/kotlin)

Slumber is aware of the Null Safety that is built into Kotlin.

Pure Java serialization libraries like [Jackson](https://github.com/FasterXML/jackson) are not.

Kotlin handles the [Billion Dollar Mistake](https://en.wikipedia.org/wiki/Tony_Hoare#Apologies_and_retractions)
very well. And so should your serialization framework! Slumber does it for you.

# TODOs:

( ) Slumber
  ( ) Full java time support 

( ) Mutator
  ( ) support better code generation for nested classes
