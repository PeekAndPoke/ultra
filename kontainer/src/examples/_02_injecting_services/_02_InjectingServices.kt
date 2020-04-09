package de.peekandpoke.ultra.kontainer.examples._02_injecting_services

import de.peekandpoke.ultra.common.docs.ExampleChapter

@Suppress("ClassName")
class _02_InjectingServices : ExampleChapter {

    override val title = "Service Injection"

    override val examples = listOf(
        BasicInjectionExample(),
        FactoryMethodInjectionExample(),
        InjectSingletonIntoMultipleServicesExample(),
        InjectPrototypeIntoMultipleServicesExample(),
        InjectBySuperTypeExample(),
        // TODO: super type injection fails to ambiguity
        InjectAllBySuperTypeExample(),
        LazyInjectionExample(),
        LazyInjectionCycleBreakerExample(),
        LazyInjectAllBySuperTypeExample(),
        LazyInjectAllBySuperTypeWithLookUpExample()
    )
}
