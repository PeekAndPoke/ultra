package io.peekandpoke.funktor.inspect.introspection.api

import io.peekandpoke.funktor.auth.funktorAuth
import io.peekandpoke.funktor.core.appConfig
import io.peekandpoke.funktor.core.broker.OutgoingConverter
import io.peekandpoke.funktor.core.cli.cliServices
import io.peekandpoke.funktor.core.fixtures.FixtureInstaller
import io.peekandpoke.funktor.core.kontainer
import io.peekandpoke.funktor.core.lifecycle.AppLifeCycleHooks
import io.peekandpoke.funktor.core.repair.repairMan
import io.peekandpoke.funktor.inspect.introspection.services.ApiAccessDescriptor
import io.peekandpoke.funktor.rest.ApiFeature
import io.peekandpoke.funktor.rest.ApiRoutes
import io.peekandpoke.funktor.rest.docs.codeGen
import io.peekandpoke.funktor.rest.docs.docs
import io.peekandpoke.ultra.remote.ApiResponse
import java.lang.management.ManagementFactory
import java.time.Instant
import java.time.format.DateTimeFormatter

class IntrospectionApi(converter: OutgoingConverter) : ApiRoutes("introspection", converter) {

    val getLifecycleHooks = IntrospectionApiClient.GetLifecycleHooks.mount {
        docs {
            name = "List lifecycle hooks"
        }.codeGen {
            funcName = "getLifecycleHooks"
        }.authorize {
            isSuperUser()
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

    val getConfigInfo = IntrospectionApiClient.GetConfigInfo.mount {
        docs {
            name = "Get sanitized config"
        }.codeGen {
            funcName = "getConfigInfo"
        }.authorize {
            isSuperUser()
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

    val getCliCommands = IntrospectionApiClient.GetCliCommands.mount {
        docs {
            name = "List CLI commands"
        }.codeGen {
            funcName = "getCliCommands"
        }.authorize {
            isSuperUser()
        }.handle {
            val result = cliServices.commands
                .sortedBy { it.commandName }
                .map { cmd ->
                    val help = try {
                        cmd.getFormattedHelp() ?: ""
                    } catch (_: Exception) {
                        ""
                    }
                    CliCommandInfo(name = cmd.commandName, help = help)
                }

            ApiResponse.ok(result)
        }
    }

    val getFixtures = IntrospectionApiClient.GetFixtures.mount {
        docs {
            name = "List fixture loaders"
        }.codeGen {
            funcName = "getFixtures"
        }.authorize {
            isSuperUser()
        }.handle {
            val installer = call.kontainer.getOrNull(FixtureInstaller::class)
            val loaders = installer?.getLoaders() ?: emptyList()

            val result = loaders.map { loader ->
                FixtureInfo(
                    className = loader::class.qualifiedName ?: loader::class.simpleName ?: "?",
                    dependsOn = loader.dependsOn.map { it::class.qualifiedName ?: it::class.simpleName ?: "?" },
                )
            }

            ApiResponse.ok(result)
        }
    }

    val getRepairs = IntrospectionApiClient.GetRepairs.mount {
        docs {
            name = "List registered repairs"
        }.codeGen {
            funcName = "getRepairs"
        }.authorize {
            isSuperUser()
        }.handle {
            val result = repairMan.repairs.map { RepairInfo(className = it::class.simpleName ?: "?") }

            ApiResponse.ok(result)
        }
    }

    val getAllEndpoints = IntrospectionApiClient.GetAllEndpoints.mount {
        docs {
            name = "List all API endpoints"
        }.codeGen {
            funcName = "getAllEndpoints"
        }.authorize {
            isSuperUser()
        }.handle {
            val features = call.kontainer.getAll(ApiFeature::class)

            val result = features.flatMap { feature ->
                feature.getRouteGroups().flatMap { group ->
                    group.all.map { route ->
                        EndpointInfo(
                            feature = feature.name,
                            group = group.name,
                            method = route.method.value,
                            uri = route.pattern.pattern,
                            authDescription = route.authRules.joinToString(", ") { it.description },
                        )
                    }
                }
            }

            ApiResponse.ok(result)
        }
    }

    val getAuthRealms = IntrospectionApiClient.GetAuthRealms.mount {
        docs {
            name = "List auth realms"
        }.codeGen {
            funcName = "getAuthRealms"
        }.authorize {
            isSuperUser()
        }.handle {
            val realms = funktorAuth.realms.map { realm ->
                AuthRealmInfo(
                    id = realm.id,
                    providers = realm.providers.map { provider ->
                        AuthProviderInfo(
                            id = provider.id,
                            type = provider::class.simpleName ?: "?",
                            capabilities = provider.capabilities.map { it::class.simpleName ?: "?" },
                        )
                    },
                    passwordPolicyRegex = realm.passwordPolicy.regex,
                    passwordPolicyDescription = realm.passwordPolicy.description,
                )
            }

            ApiResponse.ok(realms)
        }
    }

    val validatePassword = IntrospectionApiClient.PostValidatePassword.mount {
        docs {
            name = "Validate password against policy"
        }.codeGen {
            funcName = "validatePassword"
        }.authorize {
            isSuperUser()
        }.handle { body ->
            val realm = funktorAuth.realms.firstOrNull()
            val policy = realm?.passwordPolicy

            val matches = policy?.matches(body.password) ?: true

            ApiResponse.ok(
                PasswordValidationResponse(
                    matches = matches,
                    policyDescription = policy?.description ?: "No policy configured",
                )
            )
        }
    }

    val getAppLifecycle = IntrospectionApiClient.GetAppLifecycle.mount {
        docs {
            name = "Get app lifecycle info"
        }.codeGen {
            funcName = "getAppLifecycle"
        }.authorize {
            isSuperUser()
        }.handle {
            val mxBean = ManagementFactory.getRuntimeMXBean()
            val startTime = Instant.ofEpochMilli(mxBean.startTime)

            ApiResponse.ok(
                AppLifecycleInfo(
                    startedAt = DateTimeFormatter.ISO_INSTANT.format(startTime),
                    uptimeSeconds = mxBean.uptime / 1000,
                    jvmVersion = System.getProperty("java.version") ?: "?",
                    kotlinVersion = KotlinVersion.CURRENT.toString(),
                )
            )
        }
    }

    val getApiAccessMatrix = IntrospectionApiClient.GetApiAccessMatrix.mount {
        docs {
            name = "API Access Matrix"
        }.codeGen {
            funcName = "getApiAccessMatrix"
        }.authorize {
            isSuperUser()
        }.handle {
            val descriptor = call.kontainer.get(ApiAccessDescriptor::class)

            ApiResponse.ok(descriptor.describe())
        }
    }
}
