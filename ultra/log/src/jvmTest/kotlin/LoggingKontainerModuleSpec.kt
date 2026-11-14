package io.peekandpoke.ultra.log

import io.kotest.core.spec.style.StringSpec
import io.kotest.assertions.assertSoftly
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.kotest.matchers.types.shouldBeSameInstanceAs
import io.kotest.matchers.types.shouldNotBeSameInstanceAs
import io.peekandpoke.ultra.kontainer.ServiceKind
import io.peekandpoke.ultra.kontainer.ServiceType
import io.peekandpoke.ultra.kontainer.kontainer

class LoggingKontainerModuleSpec : StringSpec() {

    class ServiceWithLog(val log: Log)

    class NoLogService

    class TwoLogs(@Suppress("unused") val first: Log, @Suppress("unused") val second: Log)

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

        // Pins the promotion described in ULTRA-00149 (log scan findings) L11. This is
        // NOT desired behaviour - it is recorded so the surprise is visible rather than silent. Declaring
        // Log as `dynamicPrototype` names it but keeps it; whether Log stays dynamic is ULTRA-00158.
        "KNOWN: injecting a Log promotes a singleton to SemiDynamic" {

            val kontainer = kontainer {
                ultraLogging()
                singleton(ServiceWithLog::class)
            }.create()

            kontainer.getProvider(ServiceWithLog::class).type shouldBe ServiceType.Singleton
            kontainer.getProvider(ServiceWithLog::class).kind shouldBe ServiceKind.SemiDynamic
        }

        "KNOWN: a service without a Log stays a true Singleton" {

            val kontainer = kontainer {
                ultraLogging()
                singleton(NoLogService::class)
            }.create()

            kontainer.getProvider(NoLogService::class).type shouldBe ServiceType.Singleton
            kontainer.getProvider(NoLogService::class).kind shouldBe ServiceKind.Static
        }

        "Log is a dynamic prototype" {

            val kontainer = kontainer {
                ultraLogging()
            }.create()

            kontainer.getProvider(Log::class).type shouldBe ServiceType.Prototype
            kontainer.getProvider(Log::class).kind shouldBe ServiceKind.Dynamic
        }

        // Counts what is created per kontainer, so a change of how Log is declared is proven to change nothing.
        "A Log is created per injection point and per get, inside the kontainer it is injected in" {

            val blueprint = kontainer {
                ultraLogging()
                singleton(ServiceWithLog::class)
                prototype(TwoLogs::class)
            }

            val one = blueprint.create()
            val two = blueprint.create()

            one.get(ServiceWithLog::class)
            one.get(ServiceWithLog::class)
            val twoLogs = one.get(TwoLogs::class)
            one.get(Log::class)
            two.get(ServiceWithLog::class)

            assertSoftly {
                one.getProvider(Log::class).instances.size shouldBe 4
                two.getProvider(Log::class).instances.size shouldBe 1

                twoLogs.first shouldNotBeSameInstanceAs twoLogs.second
                one.get(ServiceWithLog::class).log shouldBeSameInstanceAs one.get(ServiceWithLog::class).log
                one.get(ServiceWithLog::class) shouldNotBeSameInstanceAs two.get(ServiceWithLog::class)

                one.getProvider(UltraLogManager::class).instances.size shouldBe 1
                two.getProvider(UltraLogManager::class).instances.size shouldBe 1
                one.get(UltraLogManager::class) shouldNotBeSameInstanceAs two.get(UltraLogManager::class)
            }
        }
    }
}
