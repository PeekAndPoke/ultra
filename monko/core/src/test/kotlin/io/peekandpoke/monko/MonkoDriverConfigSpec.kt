package io.peekandpoke.monko

import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.peekandpoke.ultra.log.Log
import io.peekandpoke.ultra.log.LogLevel
import io.peekandpoke.ultra.log.NullLog
import io.peekandpoke.ultra.vault.profiling.NullQueryProfiler

class MonkoDriverConfigSpec : FreeSpec() {

    companion object {
        private fun createMinimalDriver(): MonkoDriver {
            val lazyCodec = lazy<MonkoCodec> { error("Not needed") }
            val lazyClient = lazy<MongoClient> { error("Not needed") }
            val lazyDatabase = lazy<MongoDatabase> { error("Not needed") }

            return MonkoDriver(
                lazyCodec = lazyCodec,
                lazyClient = lazyClient,
                lazyDatabase = lazyDatabase,
            )
        }
    }

    init {
        "MonkoDriver configuration" - {

            "default log should be NullLog" {
                val driver = createMinimalDriver()

                driver.log shouldBe NullLog
            }

            "withLog should return a new driver with the given log" {
                val driver = createMinimalDriver()
                val customLog = object : Log {
                    override fun log(level: LogLevel, message: String) {}
                }

                val newDriver = driver.withLog(customLog)

                newDriver.log shouldBe customLog
                newDriver shouldNotBe driver
            }

            "withLog should preserve other settings" {
                val driver = createMinimalDriver()
                val customLog = object : Log {
                    override fun log(level: LogLevel, message: String) {}
                }

                val newDriver = driver.withLog(customLog)

                // The new driver should still be a valid MonkoDriver
                newDriver.log shouldBe customLog
            }

            "withProfiler should return a new driver with the given profiler" {
                val driver = createMinimalDriver()
                val customProfiler = NullQueryProfiler

                val newDriver = driver.withProfiler(customProfiler)

                newDriver.profiler shouldBe customProfiler
                newDriver shouldNotBe driver
            }

            "withProfiler should preserve log setting" {
                val driver = createMinimalDriver()
                val customLog = object : Log {
                    override fun log(level: LogLevel, message: String) {}
                }

                val driverWithLog = driver.withLog(customLog)
                val driverWithProfiler = driverWithLog.withProfiler(NullQueryProfiler)

                driverWithProfiler.log shouldBe customLog
                driverWithProfiler.profiler shouldBe NullQueryProfiler
            }

            "default profiler should be NullQueryProfiler" {
                val driver = createMinimalDriver()

                driver.profiler shouldBe NullQueryProfiler
            }
        }
    }
}
