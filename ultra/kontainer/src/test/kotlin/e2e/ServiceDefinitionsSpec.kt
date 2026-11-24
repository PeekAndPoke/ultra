package io.peekandpoke.ultra.kontainer.e2e

import io.kotest.assertions.assertSoftly
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.peekandpoke.ultra.kontainer.KontainerBuilder
import io.peekandpoke.ultra.kontainer.kontainer

class ServiceDefinitionsSpec : StringSpec() {

    private fun setupService(builder: KontainerBuilder) {
        with(builder) {
            singleton(CounterService::class)
        }
    }

    private fun overrideService(builder: KontainerBuilder) {
        with(builder) {
            singleton(CounterService::class, CounterServiceEx01::class)
        }
    }

    private fun setupDynamicService(builder: KontainerBuilder) {
        with(builder) {
            dynamic(CounterService::class)
        }
    }

    init {

        "Code location of a service definition must be recorded correctly" {

            val subject = kontainer {
                setupService(this)
            }

            subject.definitions[CounterService::class]!!.codeLocation.stack.let { stack ->

                println("=== Stack of service ===")

                stack.forEach { println(it) }

                val first = stack.first()

                withClue("ClassName of the first stack entry must be correct") {
                    first.className.shouldStartWith(ServiceDefinitionsSpec::class.qualifiedName!!)
                }

                withClue("MethodName of the first stack entry must be correct") {
                    first.methodName shouldBe ::setupService.name
                }
            }
        }

        "Code location of an overwritten service definition must be recorded correctly" {

            val subject = kontainer {
                setupService(this)
                overrideService(this)
            }

            subject.definitions[CounterService::class]!!.let { def ->

                def.codeLocation.stack.let { stack ->

                    println("=== Stack of resulting service ===")

                    stack.forEach { println(it) }

                    val first = stack.first()

                    withClue("Resulting Service: ClassName of the first stack entry must be correct") {
                        first.className.shouldStartWith(ServiceDefinitionsSpec::class.qualifiedName!!)
                    }

                    withClue("Resulting Service: MethodName of the first stack entry must be correct") {
                        first.methodName shouldBe ::overrideService.name
                    }
                }

                withClue("The overwritten service definition must be present") {
                    def.overwrites.shouldNotBeNull()
                }

                def.overwrites!!.codeLocation.stack.let { stack ->
                    println("=== Stack of overwritten service ===")

                    stack.forEach { println(it) }

                    val first = stack.first()

                    withClue("Overwritten Service: ClassName of the first stack entry must be correct") {
                        first.className.shouldStartWith(ServiceDefinitionsSpec::class.qualifiedName!!)
                    }

                    withClue("Overwritten Service: MethodName of the first stack entry must be correct") {
                        first.methodName shouldBe ::setupService.name
                    }
                }

                withClue("The overwritten service itself did not overwrite anything") {
                    def.overwrites.overwrites.shouldBeNull()
                }
            }
        }

        // ULTRA-00353: an override's location is the class of its lambda, no stack is captured
        "Code location of an override names the class of the override lambda, and nothing else" {

            val override: () -> CounterService = { CounterServiceEx01() }

            val subject = kontainer {
                setupDynamicService(this)
            }.create {
                with(CounterService::class, override)
            }

            val definition = subject.getProvider(CounterService::class).definition
            val info = subject.tools.getDebugInfo().services.first { it.cls.fqn == CounterService::class.java.name }

            assertSoftly {
                definition.codeLocation.stack.map { it.className } shouldBe listOf(override.javaClass.name)
                definition.codeLocation.first()?.className shouldBe override.javaClass.name
                info.definition.codeLocation.location shouldStartWith "${override.javaClass.name}."

                withClue("The overwritten definition keeps the location where the blueprint defined it") {
                    definition.overwrites?.codeLocation?.first()?.methodName shouldBe ::setupDynamicService.name
                }
            }
        }

        "Code location of an override written in place points at the class where with { } was written" {

            val subject = kontainer {
                setupDynamicService(this)
            }.create {
                with(CounterService::class) { CounterServiceEx02() }
            }

            val info = subject.tools.getDebugInfo().services.first { it.cls.fqn == CounterService::class.java.name }

            info.definition.codeLocation.location shouldStartWith ServiceDefinitionsSpec::class.qualifiedName!!
        }
    }
}
