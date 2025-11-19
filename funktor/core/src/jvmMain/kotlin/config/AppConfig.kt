package de.peekandpoke.funktor.core.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import de.peekandpoke.funktor.core.config.funktor.FunktorConfig
import de.peekandpoke.funktor.core.config.ktor.KtorConfig
import de.peekandpoke.ultra.common.reflection.TypeRef
import de.peekandpoke.ultra.common.reflection.kType
import de.peekandpoke.ultra.slumber.Codec
import de.peekandpoke.ultra.slumber.awake
import java.io.File

/**
 * AppConfig is an interface that provides access to application configurations.
 */
interface AppConfig {
    companion object {
        private const val DEFAULT_FILE = "config/application.{env}.conf"

        /**
         * Represents an instance of `AppConfig` that has no configuration applied.
         */
        val empty: AppConfig = NullAppConfig

        /**
         * Creates a new instance of `AppConfig` with the specified configuration.
         */
        fun of(
            ktor: KtorConfig = KtorConfig(),
            funktor: FunktorConfig = FunktorConfig(),
            keys: Map<String, String> = emptyMap(),
        ): AppConfig {
            return AppConfigImpl(ktor, funktor, keys)
        }

        /**
         * Loads the application configuration based on the specified environment.
         *
         * @param env    The environment for which the configuration should be loaded (e.g., "dev", "live", "qa").
         * @param loader The class loader used to locate the configuration file from resources.
         *               Defaults to the current thread's context class loader.
         *
         * @return The loaded configuration as a [Config] object.
         */
        @Throws(ConfigError::class)
        fun loadEnv(
            env: String,
            loader: ClassLoader = Thread.currentThread().contextClassLoader,
        ): Config {
            return loadFile(filename = getFilename(env = env), loader = loader)
        }

        /**
         * Loads the application configuration based on the specified environment.
         *
         * @param filename The config file to load.
         * @param loader   The class loader used to locate the configuration file from resources.
         *                 Defaults to the current thread's context class loader.
         *
         * @return The loaded configuration as a [Config] object.
         */
        @Throws(ConfigError::class)
        fun loadFile(
            filename: String,
            loader: ClassLoader = Thread.currentThread().contextClassLoader,
        ): Config {
            return loadRawInternal(filename = filename, loader = loader)
        }

        /**
         * Converts a `Config` instance into a strongly typed configuration object of the specified type [T].
         */
        @Throws(ConfigError::class)
        inline fun <reified T : AppConfig> Config.typed() = typed(type = kType())

        /**
         * Converts a `Config` instance into a strongly typed configuration object of the specified type [T].
         */
        @Throws(ConfigError::class)
        fun <T : AppConfig> Config.typed(type: TypeRef<T>) = of(type, this)

        /**
         * Converts a `Config` instance into a strongly typed configuration object of the specified type [T].
         */
        @Throws(ConfigError::class)
        inline fun <reified T : AppConfig> of(config: Config): T {
            return of(type = kType(), config = config)
        }

        /**
         * Converts a `Config` instance into a strongly typed configuration object of the specified type [T].
         */
        @Throws(ConfigError::class)
        fun <T : AppConfig> of(type: TypeRef<T>, config: Config): T {
            return of(type = type, config = config.root().unwrapped())
        }

        /**
         * Converts a `Config` instance into a strongly typed configuration object of the specified type [T].
         */
        @Throws(ConfigError::class)
        fun <T : AppConfig> of(type: TypeRef<T>, config: Map<String, Any?>): T {
            return try {
                Codec.default.awake(type = type, data = config)!!
            } catch (t: Throwable) {
                throw ConfigError(message = "Cannot create AppConfig of type $type", cause = t)
            }
        }

        /**
         * Generates a configuration file name by replacing a placeholder in the default file name
         * with the provided environment string.
         */
        private fun getFilename(env: String) = DEFAULT_FILE.replace("{env}", env)

        @PublishedApi
        internal fun loadRawInternal(filename: String, loader: ClassLoader): Config {

            val sources = listOf(
                "File" to File(filename).absoluteFile.takeIf { it.exists() },
                "Resource" to loader.getResource(filename)?.file?.let { File(it) },
            )

            val source = sources
                .onEach { println("Source: ${it.first} -> ${it.second}") }
                .firstOrNull { it.second != null }

            val file = source?.second

            if (file == null) {
                val error = """
                    No config file found '$filename'
                """.trimIndent()

                error(error)
            }

            println("Will load config from '${source.first}' -> '${file.absolutePath}'")

            return ConfigFactory.parseFile(file).resolve()
        }
    }

    @Suppress("unused")
    private object NullAppConfig : AppConfig {
        override val ktor = KtorConfig()
        override val funktor = FunktorConfig()
        override val keys: Map<String, String> = emptyMap()
    }

    private class AppConfigImpl(
        override val ktor: KtorConfig,
        override val funktor: FunktorConfig,
        override val keys: Map<String, String>,
    ) : AppConfig

    /**
     * Configuration for the Ktor server.
     */
    val ktor: KtorConfig

    /**
     * Configuration for Funktor-specific functionality used within the application.
     */
    val funktor: FunktorConfig

    /**
     * A map of key-value pairs user fr
     */
    val keys: Map<String, String>

    /**
     * Get the [key] or throw an [IllegalStateException] when the key is not found.
     */
    @Throws(IllegalStateException::class)
    fun getKey(key: String) = getKeyOrNull(key) ?: error("No key found for '$key'")

    /**
     * Get the [key] or return null when the key is not found.
     */
    fun getKeyOrNull(key: String) = keys[key]
}
