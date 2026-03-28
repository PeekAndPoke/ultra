package io.peekandpoke.funktor.demo.server.api.showcase

import io.peekandpoke.funktor.core.appConfig
import io.peekandpoke.funktor.core.broker.OutgoingConverter
import io.peekandpoke.funktor.core.cli.cliServices
import io.peekandpoke.funktor.core.fixtures.FixtureInstaller
import io.peekandpoke.funktor.core.kontainer
import io.peekandpoke.funktor.core.lifecycle.AppLifeCycleHooks
import io.peekandpoke.funktor.core.repair.repairMan
import io.peekandpoke.funktor.demo.common.showcase.CliCommandInfo
import io.peekandpoke.funktor.demo.common.showcase.ConfigInfoEntry
import io.peekandpoke.funktor.demo.common.showcase.FixtureInfo
import io.peekandpoke.funktor.demo.common.showcase.LifecycleHookInfo
import io.peekandpoke.funktor.demo.common.showcase.RepairInfo
import io.peekandpoke.funktor.demo.common.showcase.RetryAttemptLog
import io.peekandpoke.funktor.demo.common.showcase.RetryDemoResponse
import io.peekandpoke.funktor.demo.common.showcase.ShowcaseApiClient
import io.peekandpoke.funktor.rest.ApiRoutes
import io.peekandpoke.funktor.rest.docs.codeGen
import io.peekandpoke.funktor.rest.docs.docs
import io.peekandpoke.ultra.remote.ApiResponse
import kotlinx.coroutines.delay

class CoreShowcaseApi(converter: OutgoingConverter) : ApiRoutes("showcase-core", converter) {

    val getLifecycleHooks = ShowcaseApiClient.GetLifecycleHooks.mount {
        docs {
            name = "List lifecycle hooks"
        }.codeGen {
            funcName = "getLifecycleHooks"
        }.authorize {
            public()
        }.handle {
            val hooks = call.kontainer.getOrNull(AppLifeCycleHooks::class)

            val result = buildList {
                hooks?.onAppStarting?.forEach {
                    add(LifecycleHookInfo("OnAppStarting", it::class.simpleName ?: "?", it.executionOrder.priority))
                }
                hooks?.onAppStarted?.forEach {
                    add(LifecycleHookInfo("OnAppStarted", it::class.simpleName ?: "?", it.executionOrder.priority))
                }
                hooks?.onAppStopPreparing?.forEach {
                    add(
                        LifecycleHookInfo(
                            "OnAppStopPreparing",
                            it::class.simpleName ?: "?",
                            it.executionOrder.priority
                        )
                    )
                }
                hooks?.onAppStopping?.forEach {
                    add(LifecycleHookInfo("OnAppStopping", it::class.simpleName ?: "?", it.executionOrder.priority))
                }
                hooks?.onAppStopped?.forEach {
                    add(LifecycleHookInfo("OnAppStopped", it::class.simpleName ?: "?", it.executionOrder.priority))
                }
            }

            ApiResponse.ok(result)
        }
    }

    val getConfigInfo = ShowcaseApiClient.GetConfigInfo.mount {
        docs {
            name = "Get sanitized config"
        }.codeGen {
            funcName = "getConfigInfo"
        }.authorize {
            public()
        }.handle {
            val config = appConfig

            val result = listOf(
                ConfigInfoEntry("environment", config.ktor.deployment.environment),
                ConfigInfoEntry("host", config.ktor.deployment.host),
                ConfigInfoEntry("port", config.ktor.deployment.port.toString()),
                ConfigInfoEntry("isProduction", config.ktor.isProduction.toString()),
                ConfigInfoEntry("isDevelopment", config.ktor.isDevelopment.toString()),
            )

            ApiResponse.ok(result)
        }
    }

    val getCliCommands = ShowcaseApiClient.GetCliCommands.mount {
        docs {
            name = "List CLI commands"
        }.codeGen {
            funcName = "getCliCommands"
        }.authorize {
            public()
        }.handle {
            val result = cliServices.commands
                .sortedBy { it.commandName }
                .map { CliCommandInfo(name = it.commandName, help = "") }

            ApiResponse.ok(result)
        }
    }

    val getFixtures = ShowcaseApiClient.GetFixtures.mount {
        docs {
            name = "List fixture loaders"
        }.codeGen {
            funcName = "getFixtures"
        }.authorize {
            public()
        }.handle {
            val installer = call.kontainer.getOrNull(FixtureInstaller::class)
            val loaders = installer?.getLoaders() ?: emptyList()

            val result = loaders.map { loader ->
                FixtureInfo(
                    className = loader::class.simpleName ?: "?",
                    dependsOn = loader.dependsOn.map { it::class.simpleName ?: "?" },
                )
            }

            ApiResponse.ok(result)
        }
    }

    val getRepairs = ShowcaseApiClient.GetRepairs.mount {
        docs {
            name = "List registered repairs"
        }.codeGen {
            funcName = "getRepairs"
        }.authorize {
            public()
        }.handle {
            val result = repairMan.repairs.map { RepairInfo(className = it::class.simpleName ?: "?") }

            ApiResponse.ok(result)
        }
    }

    val postRetryDemo = ShowcaseApiClient.PostRetryDemo.mount {
        docs {
            name = "Run retry demo"
        }.codeGen {
            funcName = "postRetryDemo"
        }.authorize {
            public()
        }.handle { body ->
            val attempts = mutableListOf<RetryAttemptLog>()
            var finalSuccess = false

            for (attempt in 1..body.maxAttempts) {
                val start = System.currentTimeMillis()

                if (attempt > 1) {
                    delay(body.delayMs)
                }

                val succeeded = attempt >= body.failUntilAttempt
                val duration = System.currentTimeMillis() - start

                attempts.add(
                    RetryAttemptLog(
                        attempt = attempt,
                        succeeded = succeeded,
                        message = if (succeeded) "Success on attempt $attempt" else "Failed on attempt $attempt",
                        durationMs = duration,
                    )
                )

                if (succeeded) {
                    finalSuccess = true
                    break
                }
            }

            ApiResponse.ok(RetryDemoResponse(attempts = attempts, finalSuccess = finalSuccess))
        }
    }
}
