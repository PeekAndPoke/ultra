package de.peekandpoke.ultra.kontainer.examples.injecting_services

import de.peekandpoke.ultra.common.docs.ExampleChapter

@Suppress("ClassName")
class _InjectingServices : ExampleChapter {

    override val title = "Service Injection"

    override val examples = listOf(
        BasicInjectionExample(),
        FactoryMethodInjectionExample(),
        InjectSingletonIntoMultipleServicesExample(),
        InjectPrototypeIntoMultipleServicesExample(),
        InjectBySuperTypeExample(),
        InjectBySuperTypeFailsToAmbiguityExample(),
        InjectAllBySuperTypeExample(),
        LazyInjectionExample(),
        LazyInjectionCycleBreakerExample(),
        LazyInjectAllBySuperTypeExample(),
        LazyInjectAllBySuperTypeWithLookUpExample()
    )
}
