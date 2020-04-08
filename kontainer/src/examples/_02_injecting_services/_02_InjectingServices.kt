package de.peekandpoke.ultra.kontainer.examples._02_injecting_services

import de.peekandpoke.ultra.common.docs.ExampleChapter

@Suppress("ClassName")
class _02_InjectingServices : ExampleChapter {

    override val title = "Service Injection"

    override val packageLocation = "_02_injecting_services"

    override val examples = listOf(
        BasicInjectionExample(),
        FactoryMethodInjectionExample(),
        InjectSingletonIntoMultipleServicesExample(),
        InjectPrototypeIntoMultipleServicesExample(),
        InjectBySuperTypeExample(),
        InjectAllBySuperTypeExample(),
        LazyInjectionExample(),
        LazyInjectionCycleBreakerExample(),
        LazyInjectAllBySuperTypeExample(),
        LazyInjectAllBySuperTypeWithLookUpExample()
    )
}
