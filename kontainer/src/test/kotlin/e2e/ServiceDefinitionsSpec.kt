package de.peekandpoke.ultra.kontainer.e2e

import de.peekandpoke.ultra.kontainer.CounterService
import de.peekandpoke.ultra.kontainer.CounterServiceEx01
import de.peekandpoke.ultra.kontainer.KontainerBuilder
import de.peekandpoke.ultra.kontainer.kontainer
import io.kotest.assertions.withClue
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith

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
                    def.overwrites!!.overwrites.shouldBeNull()
                }
            }
        }
    }
}
