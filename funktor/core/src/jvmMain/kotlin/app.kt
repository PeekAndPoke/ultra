package de.peekandpoke.funktor.core

import de.peekandpoke.funktor.core.cli.CliRunner
import de.peekandpoke.funktor.core.config.AppConfig
import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.common.reflection.kType
import de.peekandpoke.ultra.kontainer.DynamicOverrides
import de.peekandpoke.ultra.kontainer.Kontainer
import de.peekandpoke.ultra.kontainer.KontainerBlueprint
import de.peekandpoke.ultra.security.user.UserProvider
import de.peekandpoke.ultra.vault.profiling.NullQueryProfiler
import de.peekandpoke.ultra.vault.profiling.QueryProfiler
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

/**
 * Defines the entry point for the application setup using Funktor, allowing configuration of
 * CLI interactions, server initialization, and kontainers setup.
 *
 * @param C The specific type of application configuration implementing [AppConfig].
 * @param cli A lambda that defines the command-line interface behavior for the application.
 *            Defaults to [App.Definition.defaultCli].
 * @param server A lambda that defines the server initialization logic for the application.
 *               Defaults to [App.Definition.defaultNettyServer].
 * @param kontainers A lambda that creates [AppKontainers] for dependency management based on
 *                   the provided configuration of type [C].
 *
 * @return An instance of [App.Definition] containing the application definition and configuration.
 */
inline fun <reified C : AppConfig> funktorApp(
    noinline cli: (ctx: App<C>) -> Unit = App.Definition.defaultCli,
    noinline server: (ctx: App<C>) -> Unit = App.Definition.defaultNettyServer,
    noinline kontainers: (config: C) -> AppKontainers,
): App.Definition<C> {
    return App.Definition(
        configType = kType(),
        cli = cli,
        server = server,
        kontainers = kontainers,
    )
}

/**
 * Represents the core application class responsible for managing the application lifecycle.
 *
 * @param C The type of application configuration. It must extend from AppConfig.
 * @param args The command-line arguments provided to the application.
 * @param config The configuration object of type `C` for the application.
 * @param kontainers The dependency management kontainers used within the application.
 */
class App<C : AppConfig>(
    val args: AppArgs,
    val config: C,
    val kontainers: AppKontainers,
) {
    class Definition<C : AppConfig>(
        val configType: TypeRef<C>,
        val cli: (app: App<C>) -> Unit,
        val server: (app: App<C>) -> Unit,
        val kontainers: (config: C) -> AppKontainers,
    ) {
        companion object {
            /**
             * DIRTY HACK: this is needed to inject the App into Test applications
             */
            var testApp: App<*>? = null

            /**
             * Default implementation for executing command-line interface (CLI) interactions
             */
            val defaultCli: (ctx: App<*>) -> Unit = { app ->
                CliRunner.create(app)
            }

            /**
             * A default implementation for starting a Netty-based application server using Ktor's EngineMain.
             */
            val defaultNettyServer: (ctx: App<*>) -> Unit = { app ->
                // Create the server
                val args = app.args.args.toTypedArray()
                val server = EngineMain.createServer(args)
                // Provide the app for later retrieval
                server.application.provideApp(app)
                // Start the engine
                server.start(wait = true)
            }
        }

        fun run(cmdline: Array<String>) {
            val cmd = cmdline.toList()

            println("Starting with args: $cmd")

            val args = AppArgs(cmd)

            // We employ the ktor mechanism for loading the configuration
            val ktorConfig = CommandLineConfig(args.argsArray)
            // Convert to the typed AppConfig
            val config = AppConfig.of(type = configType, config = ktorConfig.environment.config.toMap())
            // Get the env specified
            val env = config.ktor.deployment.environment
            // Set up the kontainers
            val kontainers = kontainers(config)

            // Create the app instance

            if (args.isCli()) {
                val app = App(args = args.cleanedForCli, config = config, kontainers = kontainers)

                println("Launching Cli for env '$env' with args: ${app.args.args}")

                cli(app)
            } else {
                val app = App(args = args, config = config, kontainers = kontainers)

                println("Starting Server for env '$env' with args: ${args.args}")

                server(app)
            }
        }

        /**
         * Registers a module in the application with the given initialization block.
         *
         * @param application The Ktor application instance where the module will be registered.
         * @param block A lambda function that provides initialization logic. It takes three parameters:
         *              - `app`: The `App` instance associated with the application.
         *              - `config`: The application configuration of type `C`.
         *              - `init`: The `Kontainer` instance used for dependency management.
         */
        fun module(
            application: Application,
            block: Application.(app: App<C>, config: C, init: Kontainer) -> Unit,
        ) {
            @Suppress("UNCHECKED_CAST")
            val app = (testApp ?: application.getApp()) as App<C>

            application.block(app, app.config, app.kontainers.system())
        }
    }
}

/**
 * A data class representing command-line arguments and provides utility functions to process them.
 *
 * @property args The list of command-line arguments passed to the application.
 *
 * The class provides the following functionalities:
 *
 * - Determine if the `--cli` flag is present.
 * - Extract the value of the `--env` argument if present.
 * - Extract the value of the `-config` argument if present.
 * - Provide a cleaned version of the arguments, excluding those that are not understood by Ktor.
 */
data class AppArgs(val args: List<String>) {
    companion object {
        const val cli = "--cli"
        private val configFileRegex = "-config=([^ ]+)".toRegex()
    }

    private val _isCli by lazy { args.contains(cli) }

    /**
     * Instance of [AppArgs] cleaned of all params should not be passed to the CLiRunner
     */
    val cleanedForCli by lazy {
        // NOTICE: We remove all params that would not be understood by Ktor
        AppArgs(
            args.filter { it != cli }
                .filter { !it.matches(configFileRegex) }
        )
    }

    /**
     * Args as typed array
     */
    val argsArray = args.toTypedArray()

    /**
     * Return true when the '--cli' parameter is present
     */
    fun isCli(): Boolean = _isCli
}

/**
 * The `AppKontainers` interface provides a mechanism to manage `Kontainer` instances using a
 * specified `KontainerBlueprint`. It allows the creation of custom containers with dynamic
 * overrides as well as a pre-configured system container.
 */
interface AppKontainers {
    companion object {
        operator fun invoke(blueprint: KontainerBlueprint): AppKontainers = Impl(blueprint)
    }

    private class Impl(override val blueprint: KontainerBlueprint) : AppKontainers

    val blueprint: KontainerBlueprint

    fun create(dynamics: DynamicOverrides.Builder.() -> Unit = {}): Kontainer = blueprint.create(dynamics)

    fun system(): Kontainer = create {
        // user record provider
        with { UserProvider.system() }
        // We do not profile database queries
        with { NullQueryProfiler }
//        with { LoggingQueryProfiles }
    }
}

object LoggingQueryProfiles : QueryProfiler {
    override val entries: List<QueryProfiler.Entry>
        get() = emptyList()

    override val explainQueries: Boolean
        get() = false

    private val map = mutableMapOf<String, Int>()

    override suspend fun <R> profile(
        connection: String,
        queryLanguage: String,
        query: String,
        block: suspend (QueryProfiler.Entry) -> R,
    ): R {
        synchronized(map) {
            map[query] = map.getOrDefault(query, 0) + 1
        }

        val logs = map.entries
            .sortedByDescending { (_, count) -> count }
            .take(20)
            .map { (query, count) ->
                "${count}X : ${query.replace("\n", "\\n").take(160)}"
            }

        println(
            "-- Distinct Queries: ${map.size} ------------------------------------------------------------------------------------" +
                    "\n" +
                    logs.joinToString("\n")
        )

        System.out.flush()

        return block(QueryProfiler.Entry.Null)
    }
}
