package io.peekandpoke.ultra.log

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.peekandpoke.ultra.kontainer.ServiceProvider
import io.peekandpoke.ultra.kontainer.kontainer

class LoggingKontainerModuleSpec : StringSpec() {

    class ServiceWithLog(@Suppress("unused") val log: Log)

    class NoLogService

    init {

        "Kontainer module must work" {

            val bluePrint = kontainer {
                ultraLogging()
            }

            val kontainer = bluePrint.create()

            kontainer.get(UltraLogManager::class).shouldBeInstanceOf<UltraLogManager>()
        }

        "The global minLevel reaches the manager" {

            val kontainer = kontainer {
                ultraLogging(minLevel = LogLevel.WARNING)
                singleton(ConsoleAppender::class)
            }.create()

            val manager = kontainer.get(UltraLogManager::class)

            manager.isEnabled(LogLevel.ERROR) shouldBe true
            manager.isEnabled(LogLevel.INFO) shouldBe false
        }

        // Pins the promotion described in .claude/tasks/20260729-log-scan-findings.md L11. This is
        // NOT desired behaviour - it is recorded so the surprise is visible rather than silent. The
        // real fix is the `dynamicPrototype` injection type that index_jvm.kt asks for.
        "KNOWN: injecting a Log promotes a singleton to SemiDynamic" {

            val kontainer = kontainer {
                ultraLogging()
                singleton(ServiceWithLog::class)
            }.create()

            kontainer.getProvider(ServiceWithLog::class).type shouldBe ServiceProvider.Type.SemiDynamic
        }

        "KNOWN: a service without a Log stays a true Singleton" {

            val kontainer = kontainer {
                ultraLogging()
                singleton(NoLogService::class)
            }.create()

            kontainer.getProvider(NoLogService::class).type shouldBe ServiceProvider.Type.Singleton
        }
    }
}
