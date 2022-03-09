package de.peekandpoke.ultra.kontainer.e2e

import de.peekandpoke.ultra.kontainer.AnotherSimpleService
import de.peekandpoke.ultra.kontainer.CounterService
import de.peekandpoke.ultra.kontainer.InjectingService
import de.peekandpoke.ultra.kontainer.MyServiceInterface
import de.peekandpoke.ultra.kontainer.kontainer
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec

class KontainerToolsSpec : FreeSpec() {

    init {
        "Getting debugInfo()" - {

            "Must work in general" {

                class MyServiceImpl : MyServiceInterface

                val subject = kontainer {
                    singleton(MyServiceInterface::class, MyServiceImpl::class)
                    singleton(InjectingService::class)
                    singleton(CounterService::class)
                    prototype(AnotherSimpleService::class)
                }.create()

                subject.get(MyServiceInterface::class)
                subject.get(InjectingService::class)
                subject.get(CounterService::class)
                subject.get(AnotherSimpleService::class)
                subject.get(AnotherSimpleService::class)
                subject.get(AnotherSimpleService::class)

                withClue("Getting the debug info must work") {
                    subject.tools.getDebugInfo()
                }
            }
        }
    }
}
