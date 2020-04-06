package de.peekandpoke.ultra.kontainer.examples._02_injection

import de.peekandpoke.ultra.common.docs.ExampleChapter

@Suppress("ClassName")
class _02_Injection : ExampleChapter {

    override val title = "Service Injection"

    override val packageLocation = "_02_injection"

    override val examples = listOf(
        BasicInjectionExample(),
        FactoryMethodInjectionExample(),
        InjectSingletonIntoMultipleServicesExample(),
        InjectPrototypeIntoMultipleServicesExample(),
        InjectBySuperTypeExample(),
        InjectAllBySuperTypeExample(),
        LazyInjectionExample(),
        LazyInjectionCycleBreakerExample(),
        LazyInjectAllBySuperTypeExample()
        // TODO: Lazy injection and breaking cyclic dependencies
        // TODO: Lazy inject all services with a LookUp
    )
}
