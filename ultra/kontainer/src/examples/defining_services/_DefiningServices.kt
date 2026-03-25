package io.peekandpoke.ultra.kontainer.examples.defining_services

import io.peekandpoke.ultra.tooling.ExampleChapter

@Suppress("ClassName")
class _DefiningServices : ExampleChapter {

    override val title = "Defining Services"

    override val examples = listOf(
        DefiningASingletonExample(),
        DefiningADynamicExample(),
        DefiningAPrototypeExample(),
        DefiningAnExistingInstanceExample(),
        HidingTheConcreteImplementationOfAServiceExample(),
        SharedSingletonExample(),
        SingletonVsDynamicVsPrototypeExample()
        // TODO: retrieve a service by super type
        // TODO: retrieve a service by super type fails due to ambiguity
    )
}
