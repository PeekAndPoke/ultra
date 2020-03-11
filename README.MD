# ULTRA components for Kotlin Servers and Android

[![CodeFactor](https://www.codefactor.io/repository/github/peekandpoke/ultra/badge)](https://www.codefactor.io/repository/github/peekandpoke/ultra)

ULTRA is a set of libraries and helpers to enhance your everyday Kotlin coding with more power, stability and enjoyment.

Check out the sub-projects. There you will find details, descriptive test cases and usage examples.

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

Read more here: [common/ultra::common.md]() 


## ultra::kontainer

[utlra::kontainer](kontainer/ultra::kontainer.md) - A multi stage dependency injection mechanism  



# TODOs:

( ) Kontainer: avoid that services definitions are downgraded
  ( ) invalid: Prototype -> Singleton | Dynamic
  ( ) invalid: Dynamic -> Singleton
  ( ) invalid: Singleton | Dynamic -> Prototype  


( ) Slumber
  ( ) Full java time support 

( ) Mutator
  ( ) support better code generation for nested classes
