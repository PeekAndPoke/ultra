package de.peekandpoke.ultra.kontainer.examples._01_defining_services

import de.peekandpoke.ultra.common.docs.ExampleChapter

@Suppress("ClassName")
class _01_DefiningServices : ExampleChapter {

    override val title = "Defining Services"

    override val packageLocation = "_01_defining_services"

    override val examples = listOf(
        DefiningASingletonExample(),
        DefiningADynamicExample(),
        DefiningAPrototypeExample(),
        DefiningAnExistingInstanceExample(),
        SharedSingletonExample(),
        SingletonVsDynamicVsPrototypeExample()
        // TODO: register service by a super type
    )
}
