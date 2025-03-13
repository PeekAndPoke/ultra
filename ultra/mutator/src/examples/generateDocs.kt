package de.peekandpoke.ultra.mutator.examples

import de.peekandpoke.ultra.common.docs.ExamplesToDocs
import de.peekandpoke.ultra.mutator.examples.dataclasses._DataClassMutation
import de.peekandpoke.ultra.mutator.examples.introduction._Introduction

fun main() {
    val generator = object : ExamplesToDocs(
        title = "Examples for ultra::mutator",
        chapters = chapters
    ) {}

    generator.run()
}

val chapters = listOf(
    _Introduction(),
    _DataClassMutation()

    // TODO: Chapter "Object mutation"
    //    -> Mutating primitive fields and nullable primitive fields
    //    -> Mutating enum fields and nullable enum fields
    //    -> Mutating nested objects and nullable nested objects
    //    -> Mutating generic types
    // TODO: Chapter "Mutator Inheritance hierarchy"
    // TODO: Chapter "List mutations"
    // TODO: Chapter "Set mutations"
    // TODO: Chapter "Map mutations"
    // TODO: Advanced - Code generation examples
)
