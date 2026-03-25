package io.peekandpoke.ultra.kontainer.e2e

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FreeSpec
import io.peekandpoke.ultra.kontainer.kontainer

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
