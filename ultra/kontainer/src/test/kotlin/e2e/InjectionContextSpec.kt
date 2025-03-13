package de.peekandpoke.ultra.kontainer.e2e

import de.peekandpoke.ultra.common.Lookup
import de.peekandpoke.ultra.kontainer.InjectionContext
import de.peekandpoke.ultra.kontainer.Kontainer
import de.peekandpoke.ultra.kontainer.ServiceProvider
import de.peekandpoke.ultra.kontainer.kontainer
import io.kotest.assertions.assertSoftly
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlin.reflect.KClass

class InjectionContextSpec : StringSpec({

    "Injection context points to Kontainer, when service is directly requested from the Kontainer" {

        data class MyInjected(val context: InjectionContext)

        val blueprint = kontainer {

            prototype(MyInjected::class) { context: InjectionContext -> MyInjected(context) }
        }

        val subject = blueprint.create()

        assertSoftly {
            subject.get(MyInjected::class).context.requestingClass shouldBe Kontainer::class

            subject.get(MyInjected::class).context.injectingClass shouldBe MyInjected::class
        }
    }

    "Using the injection context when injecting a service" {

        data class MyInjected(val context: InjectionContext)

        data class MyInjector(val injected: MyInjected)

        val blueprint = kontainer {

            prototype(MyInjector::class)

            prototype(MyInjected::class) { context: InjectionContext ->
                MyInjected(context)
            }
        }

        val subject = blueprint.create()

        assertSoftly {
            subject.get(MyInjected::class).let {
                it.context.requestingClass shouldBe Kontainer::class
                it.context.injectingClass shouldBe MyInjected::class
            }

            subject.get(MyInjector::class).let {
                it.injected.context.requestingClass shouldBe MyInjector::class
                it.injected.context.injectingClass shouldBe MyInjected::class
            }
        }
    }

    "Using the injection context when injecting a service multiple levels" {

        data class MyInner(val injector: KClass<*>)

        data class MyMiddle(val inner: MyInner)

        data class MyOuter(val middle: MyMiddle)

        val blueprint = kontainer {

            prototype(MyOuter::class)
            prototype(MyMiddle::class)

            prototype(MyInner::class) { context: InjectionContext -> MyInner(context.requestingClass) }
        }

        val subject = blueprint.create()

        assertSoftly {
            subject.get(MyInner::class).injector shouldBe Kontainer::class

            subject.get(MyOuter::class).middle.inner.injector shouldBe MyMiddle::class
        }
    }

    "Using the injection context when injecting a nullable service" {

        data class MyInjected(val injector: KClass<*>)

        data class MyInjector(val injected: MyInjected?)

        val blueprint = kontainer {

            prototype(MyInjector::class)

            prototype(MyInjected::class) { context: InjectionContext -> MyInjected(context.requestingClass) }
        }

        val subject = blueprint.create()

        assertSoftly {
            subject.get(MyInjected::class).injector shouldBe Kontainer::class

            subject.get(MyInjector::class).injected!!.injector shouldBe MyInjector::class
        }
    }

    "Using the injection context when injecting multiple services as list" {

        abstract class MyBase(val injector: KClass<*>)
        class MyInjectedOne(injector: KClass<*>) : MyBase(injector)
        class MyInjectedTwo(injector: KClass<*>) : MyBase(injector)

        data class MyInjector(val all: List<MyBase>)

        val blueprint = kontainer {

            prototype(MyInjector::class)

            prototype(MyInjectedOne::class) { context: InjectionContext -> MyInjectedOne(context.requestingClass) }
            prototype(MyInjectedTwo::class) { context: InjectionContext -> MyInjectedTwo(context.requestingClass) }
        }

        val subject = blueprint.create()

        assertSoftly {
            subject.get(MyInjectedOne::class).injector shouldBe Kontainer::class
            subject.get(MyInjectedTwo::class).injector shouldBe Kontainer::class

            subject.get(MyInjector::class).all[0].injector shouldBe MyInjector::class
            subject.get(MyInjector::class).all[1].injector shouldBe MyInjector::class
        }
    }

    "Using the injection context when injecting multiple services as lookup" {

        abstract class MyBase(val injector: KClass<*>)
        class MyInjectedOne(injector: KClass<*>) : MyBase(injector)
        class MyInjectedTwo(injector: KClass<*>) : MyBase(injector)

        data class MyInjector(val all: Lookup<MyBase>)

        val blueprint = kontainer {

            prototype(MyInjector::class)

            prototype(MyInjectedOne::class) { context: InjectionContext -> MyInjectedOne(context.requestingClass) }
            prototype(MyInjectedTwo::class) { context: InjectionContext -> MyInjectedTwo(context.requestingClass) }
        }

        val subject = blueprint.create()

        assertSoftly {
            subject.get(MyInjectedOne::class).injector shouldBe Kontainer::class
            subject.get(MyInjectedTwo::class).injector shouldBe Kontainer::class

            subject.get(MyInjector::class).all.all()[0].injector shouldBe MyInjector::class
            subject.get(MyInjector::class).all.all()[1].injector shouldBe MyInjector::class
        }
    }

    "A singleton injecting the InjectionContext must become a dynamic singleton" {
        data class MyInjected(val context: InjectionContext)

        data class MyInjector(val injected: MyInjected)

        val blueprint = kontainer {

            singleton(MyInjector::class)

            singleton(MyInjected::class) { context: InjectionContext ->
                MyInjected(context)
            }
        }

        val kontainer = blueprint.create()

        assertSoftly {

            withClue("MyInjected injects the InjectionContext and must be promoted to a semi dynamic service") {
                kontainer.getProvider(MyInjected::class).type shouldBe ServiceProvider.Type.SemiDynamic
            }

            withClue("MyInjector must be promoted to a semi dynamic service as it injects MyInjected") {
                kontainer.getProvider(MyInjector::class).type shouldBe ServiceProvider.Type.SemiDynamic
            }
        }
    }
})
