package de.peekandpoke.ultra.kontainer

/** Base exception for all Kontainer errors */
open class KontainerException(message: String) : Throwable(message)

/** Thrown when a service factory is invalid */
class InvalidServiceFactory(message: String) : KontainerException(message)

/** Thrown when an invalid class (e.g. interface or abstract) is provided as a service */
class InvalidClassProvided(message: String) : KontainerException(message)

/** Thrown when the Kontainer configuration is inconsistent */
class KontainerInconsistent(message: String) : KontainerException(message)

/** Thrown when a requested service is not registered */
class ServiceNotFound(message: String) : KontainerException(message)

/** Thrown when a requested service has multiple matching candidates */
class ServiceAmbiguous(message: String) : KontainerException(message)
