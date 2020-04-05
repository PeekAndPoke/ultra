package de.peekandpoke.ultra.kontainer.examples._01_the_basics

import de.peekandpoke.ultra.common.docs.ExampleChapter

@Suppress("ClassName")
class _01_TheBasics : ExampleChapter {

    override val title = "The Basics"

    override val packageLocation = "_01_the_basics"

    override val examples = listOf(
        BasicSingletonExample(),
        SharedSingletonExample(),
        SingletonVsDynamicVsPrototypeExample()
    )
}
