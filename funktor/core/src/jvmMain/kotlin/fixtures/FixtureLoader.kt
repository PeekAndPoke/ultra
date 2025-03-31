package de.peekandpoke.funktor.core.fixtures

import de.peekandpoke.ultra.common.model.Message

/**
 * Common interface for all Fixture loaders
 */
interface FixtureLoader {

    /**
     * Returns all fixture loaders that should be executed before this one.
     */
    val dependsOn: List<FixtureLoader> get() = listOf()

    /**
     * Returns all fixtures loaders that should be executed after this one.
     */
    val completesWith: List<Lazy<FixtureLoader>> get() = listOf()

    /**
     * Gives the loader the chance for some preparation, e.g. cleaning up database tables.
     *
     * The [FixtureInstaller] will first call all [prepare] methods of all loaders.
     * Afterwards all [load] methods will be called.
     */
    suspend fun prepare(result: MutableResult) {}

    /**
     * Does the actual work of installing fixtures
     */
    suspend fun load(result: MutableResult) {}

    /**
     * Does eventual clean up work
     */
    suspend fun finalize(result: MutableResult) {}

    /**
     * Result with message about the fixture installation
     */
    interface Result {
        /** The [FixtureLoader] that produces this result */
        val loader: FixtureLoader

        /** The [Message]s produces while installing the Fixtures */
        val messages: List<Message>

        /** Returns 'true' when installing the fixtures was successful without errors */
        val isOk: Boolean
    }

    /**
     * Mutable implementation of [Result]
     */
    class MutableResult(override val loader: FixtureLoader) : Result {
        /** List of all collected messages */
        private val _messages = mutableListOf<Message>()

        /** Exposing messages as immutable list */
        override val messages get() = _messages.toList()

        /** Returns 'true' when there where no errors */
        override val isOk: Boolean
            get() = messages.all { it.type != Message.Type.error }

        /** Adds an info message */
        fun info(message: String) = apply {
            _messages.add(message.asInfo)
        }

        /** Adds multiple info messages */
        fun info(messages: List<String>) = apply {
            messages.forEach { info(it) }
        }

        /** Adds an error message */
        fun error(message: String) = apply {
            _messages.add(message.asError)
        }

        /** Adds multiple error messages */
        fun error(messages: List<String>) = apply {
            messages.forEach { error(it) }
        }

        /** Converts a string into an info message */
        private val String.asInfo get() = Message.info(this)

        /** Converts a string into an error message */
        private val String.asError get() = Message.error(this)
    }
}
